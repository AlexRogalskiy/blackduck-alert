/**
 * alert-issuetracker
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
package com.synopsys.integration.alert.issuetracker.jira.common.util;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.exception.AlertFieldException;
import com.synopsys.integration.alert.common.message.model.ComponentItem;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.alert.issuetracker.IssueContentModel;
import com.synopsys.integration.alert.issuetracker.IssueHandler;
import com.synopsys.integration.alert.issuetracker.IssueProperties;
import com.synopsys.integration.alert.issuetracker.config.IssueConfig;
import com.synopsys.integration.alert.issuetracker.message.IssueTrackerMessageParser;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jira.common.cloud.builder.IssueRequestModelFieldsBuilder;
import com.synopsys.integration.jira.common.model.request.builder.IssueRequestModelFieldsMapBuilder;
import com.synopsys.integration.jira.common.model.response.IssueResponseModel;
import com.synopsys.integration.rest.exception.IntegrationRestException;

public abstract class JiraIssueHandler extends IssueHandler<IssueResponseModel> {
    public static final String DESCRIPTION_CONTINUED_TEXT = "(description continued...)";

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private final Gson gson;
    private final JiraTransitionHandler jiraTransitionHelper;
    private final JiraIssuePropertyHandler jiraIssuePropertyHelper;

    public JiraIssueHandler(IssueTrackerMessageParser messageParser, Gson gson, JiraTransitionHandler jiraTransitionHandler, JiraIssuePropertyHandler<?> jiraIssuePropertyHandler) {
        super(messageParser);
        this.gson = gson;
        this.jiraTransitionHelper = jiraTransitionHandler;
        this.jiraIssuePropertyHelper = jiraIssuePropertyHandler;
    }

    public abstract IssueResponseModel createIssue(String issueCreator, String issueType, String projectName, IssueRequestModelFieldsMapBuilder fieldsBuilder) throws IntegrationException;

    public abstract String getIssueCreatorFieldKey();

    @Override
    protected IssueResponseModel createIssue(IssueConfig jiraIssueConfig, String providerName, LinkableItem topic, LinkableItem nullableSubTopic, ComponentItem arbitraryItem, String trackingKey, IssueContentModel contentModel)
        throws IntegrationException {
        IssueRequestModelFieldsBuilder fieldsBuilder = createFieldsBuilder(contentModel);
        fieldsBuilder.setProject(jiraIssueConfig.getProjectId());
        fieldsBuilder.setIssueType(jiraIssueConfig.getIssueType());
        String issueCreator = jiraIssueConfig.getIssueCreator();

        try {
            IssueResponseModel issue = createIssue(issueCreator, jiraIssueConfig.getIssueType(), jiraIssueConfig.getProjectName(), fieldsBuilder);
            logger.debug("Created new Jira Cloud issue: {}", issue.getKey());
            String issueKey = issue.getKey();
            addIssueProperties(issueKey, providerName, topic, nullableSubTopic, arbitraryItem, trackingKey);
            addComment(issueKey, "This issue was automatically created by Alert.");
            for (String additionalComment : contentModel.getAdditionalComments()) {
                String comment = String.format("%s \n %s", DESCRIPTION_CONTINUED_TEXT, additionalComment);
                addComment(issueKey, comment);
            }
            return issue;
        } catch (IntegrationRestException e) {
            throw improveRestException(e, issueCreator);
        }
    }

    @Override
    protected boolean transitionIssue(IssueResponseModel issueModel, IssueConfig issueConfig, ItemOperation operation) throws IntegrationException {
        return jiraTransitionHelper.transitionIssueIfNecessary(issueModel.getKey(), issueConfig, operation);
    }

    @Override
    protected String createAdditionalTrackingKey(ComponentItem componentItem) {
        if (!componentItem.collapseOnCategory()) {
            LinkableItem categoryItem = componentItem.getCategoryItem();
            return categoryItem.getName() + categoryItem.getValue();
        }
        return StringUtils.EMPTY;
    }

    private AlertException improveRestException(IntegrationRestException restException, String issueCreatorEmail) {
        JsonObject responseContent = gson.fromJson(restException.getHttpResponseContent(), JsonObject.class);
        List<String> responseErrors = new ArrayList<>();
        if (null != responseContent) {
            JsonObject errors = responseContent.get("errors").getAsJsonObject();
            JsonElement reporterErrorMessage = errors.get("reporter");
            if (null != reporterErrorMessage) {
                return AlertFieldException.singleFieldError(
                    getIssueCreatorFieldKey(), String.format("There was a problem assigning '%s' to the issue. Please ensure that the user is assigned to the project and has permission to transition issues.", issueCreatorEmail)
                );
            }

            JsonArray errorMessages = responseContent.get("errorMessages").getAsJsonArray();
            for (JsonElement errorMessage : errorMessages) {
                responseErrors.add(errorMessage.getAsString());
            }
            responseErrors.add(errors.toString());
        }

        String message = restException.getMessage();
        if (!responseErrors.isEmpty()) {
            message += " | Details: " + StringUtils.join(responseErrors, ", ");
        }

        return new AlertException(message, restException);
    }

    private void addIssueProperties(String issueKey, String provider, LinkableItem topic, LinkableItem nullableSubTopic, ComponentItem componentItem, String alertIssueUniqueId) throws IntegrationException {
        LinkableItem component = componentItem.getComponent();
        Optional<LinkableItem> subComponent = componentItem.getSubComponent();

        String subTopicName = nullableSubTopic != null ? nullableSubTopic.getName() : null;
        String subTopicValue = nullableSubTopic != null ? nullableSubTopic.getValue() : null;

        IssueProperties properties = new IssueProperties(provider, topic.getName(), topic.getValue(), subTopicName, subTopicValue, componentItem.getCategory(), component.getName(), component.getValue(),
            subComponent.map(LinkableItem::getName).orElse(null), subComponent.map(LinkableItem::getValue).orElse(null), alertIssueUniqueId);

        jiraIssuePropertyHelper.addPropertiesToIssue(issueKey, properties);
    }

    private IssueRequestModelFieldsBuilder createFieldsBuilder(IssueContentModel contentModel) {
        IssueRequestModelFieldsBuilder fieldsBuilder = new IssueRequestModelFieldsBuilder();
        fieldsBuilder.setSummary(contentModel.getTitle());
        fieldsBuilder.setDescription(contentModel.getDescription());

        return fieldsBuilder;
    }
}
