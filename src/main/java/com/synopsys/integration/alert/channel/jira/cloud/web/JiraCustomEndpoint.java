/**
 * blackduck-alert
 *
 * Copyright (c) 2019 Synopsys, Inc.
 *
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements. See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership. The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.synopsys.integration.alert.channel.jira.cloud.web;

import java.util.Map;
import java.util.Optional;
import java.util.concurrent.TimeUnit;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.alert.channel.jira.cloud.JiraChannelKey;
import com.synopsys.integration.alert.channel.jira.cloud.JiraConstants;
import com.synopsys.integration.alert.channel.jira.cloud.JiraProperties;
import com.synopsys.integration.alert.channel.jira.cloud.descriptor.JiraDescriptor;
import com.synopsys.integration.alert.common.action.CustomEndpointManager;
import com.synopsys.integration.alert.common.descriptor.config.field.endpoint.ButtonEndpointResponse;
import com.synopsys.integration.alert.common.enumeration.ConfigContextEnum;
import com.synopsys.integration.alert.common.exception.AlertDatabaseConstraintException;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.persistence.accessor.ConfigurationAccessor;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationFieldModel;
import com.synopsys.integration.alert.common.rest.ResponseFactory;
import com.synopsys.integration.alert.common.rest.model.FieldValueModel;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jira.common.cloud.rest.service.JiraAppService;
import com.synopsys.integration.jira.common.cloud.rest.service.JiraCloudServiceFactory;
import com.synopsys.integration.rest.request.Response;

@Component
public class JiraCustomEndpoint extends ButtonEndpointResponse {
    private static final Logger logger = LoggerFactory.getLogger(JiraCustomEndpoint.class);

    private final JiraChannelKey jiraChannelKey;
    private final ResponseFactory responseFactory;
    private final ConfigurationAccessor configurationAccessor;
    private final Gson gson;

    @Autowired
    public JiraCustomEndpoint(JiraChannelKey jiraChannelKey, CustomEndpointManager customEndpointManager, ResponseFactory responseFactory, ConfigurationAccessor configurationAccessor, Gson gson) throws AlertException {
        super(JiraDescriptor.KEY_JIRA_CONFIGURE_PLUGIN, customEndpointManager, responseFactory);
        this.jiraChannelKey = jiraChannelKey;
        this.responseFactory = responseFactory;
        this.configurationAccessor = configurationAccessor;
        this.gson = gson;
    }

    @Override
    public Optional<ResponseEntity<String>> preprocessRequest(final Map<String, FieldValueModel> fieldValueModels) {
        final JiraProperties jiraProperties = createJiraProperties(fieldValueModels);
        try {
            final JiraCloudServiceFactory jiraServicesCloudFactory = jiraProperties.createJiraServicesCloudFactory(logger, gson);
            final JiraAppService jiraAppService = jiraServicesCloudFactory.createJiraAppService();
            final String username = jiraProperties.getUsername();
            final String accessToken = jiraProperties.getAccessToken();
            final Response response = jiraAppService.installMarketplaceApp(JiraConstants.JIRA_APP_KEY, username, accessToken);
            if (response.isStatusCodeError()) {
                return Optional.of(responseFactory.createBadRequestResponse("", "The Jira Cloud server responded with error code: " + response.getStatusCode()));
            }
            final boolean jiraPluginInstalled = isJiraPluginInstalled(jiraAppService, accessToken, username, JiraConstants.JIRA_APP_KEY);
            if (!jiraPluginInstalled) {
                return Optional.of(responseFactory.createNotFoundResponse("Was not able to confirm Jira Cloud successfully installed the Jira Cloud plugin. Please verify the installation on you Jira Cloud server."));
            }
        } catch (final IntegrationException e) {
            logger.error("There was an issue connecting to Jira Cloud", e);
            return Optional.of(responseFactory.createBadRequestResponse("", "The following error occurred when connecting to Jira Cloud: " + e.getMessage()));
        } catch (InterruptedException e) {
            logger.error("Thread was interrupted while validating jira install.", e);
            Thread.currentThread().interrupt();
            return Optional.of(responseFactory.createInternalServerErrorResponse("", "Thread was interrupted while validating Jira plugin installation: " + e.getMessage()));
        }

        return Optional.empty();
    }

    @Override
    protected String createData(final Map<String, FieldValueModel> fieldValueModels) throws AlertException {
        return "Successfully created Alert plugin on Jira Cloud server.";
    }

    private JiraProperties createJiraProperties(final Map<String, FieldValueModel> fieldValueModels) {
        final FieldValueModel fieldUrl = fieldValueModels.get(JiraDescriptor.KEY_JIRA_URL);
        final FieldValueModel fieldAccessToken = fieldValueModels.get(JiraDescriptor.KEY_JIRA_ADMIN_API_TOKEN);
        final FieldValueModel fieldUsername = fieldValueModels.get(JiraDescriptor.KEY_JIRA_ADMIN_EMAIL_ADDRESS);

        final String url = fieldUrl.getValue().orElse("");
        final String username = fieldUsername.getValue().orElse("");
        final String accessToken = getAppropriateAccessToken(fieldAccessToken);

        return new JiraProperties(url, accessToken, username);
    }

    private String getAppropriateAccessToken(final FieldValueModel fieldAccessToken) {
        final String accessToken = fieldAccessToken.getValue().orElse("");
        final boolean accessTokenSet = fieldAccessToken.isSet();
        if (StringUtils.isBlank(accessToken) && accessTokenSet) {
            try {
                return configurationAccessor.getConfigurationByDescriptorKeyAndContext(jiraChannelKey, ConfigContextEnum.GLOBAL)
                           .stream()
                           .findFirst()
                           .flatMap(configurationModel -> configurationModel.getField(JiraDescriptor.KEY_JIRA_ADMIN_API_TOKEN))
                           .flatMap(ConfigurationFieldModel::getFieldValue)
                           .orElse("");

            } catch (final AlertDatabaseConstraintException e) {
                logger.error("Unable to retrieve existing Jira configuration.");
            }
        }

        return accessToken;
    }

    private boolean isJiraPluginInstalled(JiraAppService jiraAppService, String accessToken, String username, String appKey) throws IntegrationException, InterruptedException {
        long maxTimeForChecks = 5L;
        long checkAgain = 1L;
        while (checkAgain <= maxTimeForChecks) {
            boolean foundPlugin = jiraAppService.getInstalledApp(username, accessToken, appKey).isPresent();
            if (foundPlugin) {
                return true;
            }

            TimeUnit.SECONDS.sleep(checkAgain);
            checkAgain++;
        }

        return false;
    }

}
