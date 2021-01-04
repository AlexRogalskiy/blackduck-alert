/**
 * alert-database
 *
 * Copyright (c) 2021 Synopsys, Inc.
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
package com.synopsys.integration.alert.database.job;

import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.jetbrains.annotations.Nullable;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.synopsys.integration.alert.common.persistence.model.ConfigurationFieldModel;
import com.synopsys.integration.alert.common.persistence.model.job.BlackDuckProjectDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModel;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModelBuilder;
import com.synopsys.integration.alert.common.persistence.model.job.details.AzureBoardsJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.DistributionJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.EmailJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.JiraCloudJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.JiraJobCustomFieldModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.JiraServerJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.MSTeamsJobDetailsModel;
import com.synopsys.integration.alert.common.persistence.model.job.details.SlackJobDetailsModel;
import com.synopsys.integration.alert.descriptor.api.model.ChannelKey;

@Deprecated
public class JobConfigurationModelFieldExtractorUtils {
    private static Gson gson = new GsonBuilder().setPrettyPrinting().create();

    public static DistributionJobModel convertToDistributionJobModel(
        UUID jobId,
        Map<String, ConfigurationFieldModel> configuredFieldsMap,
        OffsetDateTime createdAt,
        @Nullable OffsetDateTime lastUpdated,
        List<BlackDuckProjectDetailsModel> projectFilterDetails
    ) {
        String channelDescriptorName = extractFieldValueOrEmptyString("channel.common.channel.name", configuredFieldsMap);
        DistributionJobModelBuilder builder = DistributionJobModel.builder()
                                                  .jobId(jobId)
                                                  .enabled(extractFieldValue("channel.common.enabled", configuredFieldsMap).map(Boolean::valueOf).orElse(true))
                                                  .name(extractFieldValueOrEmptyString("channel.common.name", configuredFieldsMap))
                                                  .distributionFrequency(extractFieldValueOrEmptyString("channel.common.frequency", configuredFieldsMap))
                                                  .processingType(extractFieldValueOrEmptyString("provider.distribution.processing.type", configuredFieldsMap))
                                                  .channelDescriptorName(channelDescriptorName)
                                                  .createdAt(createdAt)
                                                  .lastUpdated(lastUpdated)

                                                  .blackDuckGlobalConfigId(extractFieldValue("provider.common.config.id", configuredFieldsMap).map(Long::valueOf).orElse(-1L))
                                                  .filterByProject(extractFieldValue("channel.common.filter.by.project", configuredFieldsMap).map(Boolean::valueOf).orElse(false))
                                                  .projectNamePattern(extractFieldValue("channel.common.project.name.pattern", configuredFieldsMap).orElse(null))
                                                  .notificationTypes(extractFieldValues("provider.distribution.notification.types", configuredFieldsMap))
                                                  .policyFilterPolicyNames(extractFieldValues("blackduck.policy.notification.filter", configuredFieldsMap))
                                                  .vulnerabilityFilterSeverityNames(extractFieldValues("blackduck.vulnerability.notification.filter", configuredFieldsMap))
                                                  .projectFilterDetails(projectFilterDetails);

        DistributionJobDetailsModel jobDetails = null;
        ChannelKey channelKey = ChannelKey.getChannelKey(channelDescriptorName);
        if (ChannelKey.AZURE_BOARDS.equals(channelKey)) {
            jobDetails = new AzureBoardsJobDetailsModel(
                extractFieldValue("channel.azure.boards.work.item.comment", configuredFieldsMap).map(Boolean::valueOf).orElse(false),
                extractFieldValueOrEmptyString("channel.azure.boards.project", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.azure.boards.work.item.type", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.azure.boards.work.item.completed.state", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.azure.boards.work.item.reopen.state", configuredFieldsMap)
            );
        } else if (ChannelKey.EMAIL.equals(channelKey)) {
            jobDetails = new EmailJobDetailsModel(
                extractFieldValueOrEmptyString("email.subject.line", configuredFieldsMap),
                extractFieldValue("project.owner.only", configuredFieldsMap).map(Boolean::valueOf).orElse(false),
                extractFieldValue("email.additional.addresses.only", configuredFieldsMap).map(Boolean::valueOf).orElse(false),
                extractFieldValueOrEmptyString("email.attachment.format", configuredFieldsMap),
                extractFieldValues("email.additional.addresses", configuredFieldsMap)
            );
        } else if (ChannelKey.JIRA_CLOUD.equals(channelKey)) {
            jobDetails = new JiraCloudJobDetailsModel(
                extractFieldValue("channel.jira.cloud.add.comments", configuredFieldsMap).map(Boolean::valueOf).orElse(false),
                extractFieldValueOrEmptyString("channel.jira.cloud.issue.creator", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.cloud.project.name", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.cloud.issue.type", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.cloud.resolve.workflow", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.cloud.reopen.workflow", configuredFieldsMap),
                extractJiraFieldMappings("channel.jira.cloud.field.mapping", configuredFieldsMap)
            );
        } else if (ChannelKey.JIRA_SERVER.equals(channelKey)) {
            jobDetails = new JiraServerJobDetailsModel(
                extractFieldValue("channel.jira.server.add.comments", configuredFieldsMap).map(Boolean::valueOf).orElse(false),
                extractFieldValueOrEmptyString("channel.jira.server.issue.creator", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.server.project.name", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.server.issue.type", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.server.resolve.workflow", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.jira.server.reopen.workflow", configuredFieldsMap),
                extractJiraFieldMappings("channel.jira.server.field.mapping", configuredFieldsMap)
            );
        } else if (ChannelKey.MS_TEAMS.equals(channelKey)) {
            jobDetails = new MSTeamsJobDetailsModel(extractFieldValueOrEmptyString("channel.msteams.webhook", configuredFieldsMap));
        } else if (ChannelKey.SLACK.equals(channelKey)) {
            jobDetails = new SlackJobDetailsModel(
                extractFieldValueOrEmptyString("channel.slack.webhook", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.slack.channel.name", configuredFieldsMap),
                extractFieldValueOrEmptyString("channel.slack.channel.username", configuredFieldsMap)
            );
        }
        builder.distributionJobDetails(jobDetails);

        return builder.build();
    }

    public static List<JiraJobCustomFieldModel> extractJiraFieldMappings(String fieldKey, Map<String, ConfigurationFieldModel> configuredFieldsMap) {
        List<String> fieldMappingStrings = extractFieldValues(fieldKey, configuredFieldsMap);
        return fieldMappingStrings.stream()
                   .map(fieldMappingString -> gson.fromJson(fieldMappingString, JiraJobCustomFieldModel.class))
                   .collect(Collectors.toList());
    }

    public static String extractFieldValueOrEmptyString(String fieldKey, Map<String, ConfigurationFieldModel> configuredFieldsMap) {
        return extractFieldValue(fieldKey, configuredFieldsMap).orElse("");
    }

    public static Optional<String> extractFieldValue(String fieldKey, Map<String, ConfigurationFieldModel> configuredFieldsMap) {
        return extractFieldValues(fieldKey, configuredFieldsMap)
                   .stream()
                   .findAny();
    }

    public static List<String> extractFieldValues(String fieldKey, Map<String, ConfigurationFieldModel> configuredFieldsMap) {
        ConfigurationFieldModel fieldModel = configuredFieldsMap.get(fieldKey);
        if (null != fieldModel) {
            return new ArrayList<>(fieldModel.getFieldValues());
        }
        return List.of();
    }

}
