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
package com.synopsys.integration.alert.channel.jira.util;

import java.util.Optional;
import java.util.Set;

import org.apache.commons.lang3.StringUtils;
import org.springframework.lang.Nullable;

import com.synopsys.integration.alert.channel.jira.JiraConstants;
import com.synopsys.integration.alert.channel.jira.model.AlertJiraIssueProperties;
import com.synopsys.integration.alert.common.message.model.ComponentItem;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.jira.common.cloud.model.response.IssueSearchResponseModel;
import com.synopsys.integration.jira.common.cloud.rest.service.IssuePropertyService;
import com.synopsys.integration.jira.common.cloud.rest.service.IssueSearchService;

public class JiraIssuePropertyHelper {
    private static final String SEARCH_CONJUNCTION = "AND";
    private static final Set<Character> ADVANCED_SEARCH_RESERVED_CHARACTERS = Set.of('\\', '+', '-', '&', '|', '!', '(', ')', '{', '}', '[', ']', '^', '~', '*', '?', ':');

    private final IssueSearchService issueSearchService;
    private final IssuePropertyService issuePropertyService;

    public JiraIssuePropertyHelper(IssueSearchService issueSearchService, IssuePropertyService issuePropertyService) {
        this.issueSearchService = issueSearchService;
        this.issuePropertyService = issuePropertyService;
    }

    public Optional<IssueSearchResponseModel> findIssues(String jiraProjectKey, String provider, LinkableItem topic, @Nullable LinkableItem subTopic, ComponentItem componentItem, String additionalKey) throws IntegrationException {
        String subTopicName = null;
        String subTopicValue = null;
        if (null != subTopic) {
            subTopicName = subTopic.getName();
            subTopicValue = subTopic.getValue();
        }

        LinkableItem component = componentItem.getComponent();
        final Optional<LinkableItem> optionalSubComponent = componentItem.getSubComponent();
        String subComponentName = optionalSubComponent.map(LinkableItem::getName).orElse(null);
        String subComponentValue = optionalSubComponent.map(LinkableItem::getValue).orElse(null);

        return findIssues(
            jiraProjectKey, provider, topic.getName(), topic.getValue(), subTopicName, subTopicValue, componentItem.getCategory(), component.getName(), component.getValue(), subComponentName, subComponentValue, additionalKey);
    }

    public Optional<IssueSearchResponseModel> findIssues(
        String jiraProjectKey,
        String provider,
        String topicName,
        String topicValue,
        String subTopicName,
        String subTopicValue,
        String category,
        String componentName,
        String componentValue,
        String subComponentName,
        String subComponentValue,
        String additionalKey
    ) throws IntegrationException {
        final StringBuilder jqlBuilder = new StringBuilder();
        jqlBuilder.append(JiraConstants.JIRA_SEARCH_KEY_JIRA_PROJECT);
        jqlBuilder.append(" = '");
        jqlBuilder.append(escapeReservedCharacters(jiraProjectKey));
        jqlBuilder.append("' ");

        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_PROVIDER, provider);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_TOPIC_NAME, topicName);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_TOPIC_VALUE, topicValue);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_SUB_TOPIC_NAME, subTopicName);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_SUB_TOPIC_VALUE, subTopicValue);

        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_CATEGORY, category);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_COMPONENT_NAME, componentName);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_COMPONENT_VALUE, componentValue);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_SUB_COMPONENT_NAME, subComponentName);
        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_SUB_COMPONENT_VALUE, subComponentValue);

        appendPropertySearchString(jqlBuilder, JiraConstants.JIRA_ISSUE_PROPERTY_OBJECT_KEY_ADDITIONAL_KEY, additionalKey);

        final String jql = jqlBuilder.toString();
        if (!jql.isBlank()) {
            final IssueSearchResponseModel issueSearchResponseModel = issueSearchService.queryForIssues(jql);
            return Optional.of(issueSearchResponseModel);
        }
        return Optional.empty();
    }

    public void addPropertiesToIssue(
        String issueKey,
        String provider,
        String topicName,
        String topicValue,
        String subTopicName,
        String subTopicValue,
        String category,
        String componentName,
        String componentValue,
        String subComponentName,
        String subComponentValue,
        String additionalKey
    ) throws IntegrationException {
        AlertJiraIssueProperties properties = new AlertJiraIssueProperties(provider, topicName, topicValue, subTopicName, subTopicValue, category, componentName, componentValue, subComponentName, subComponentValue, additionalKey);
        addPropertiesToIssue(issueKey, properties);
    }

    public void addPropertiesToIssue(String issueKey, AlertJiraIssueProperties properties) throws IntegrationException {
        issuePropertyService.setProperty(issueKey, JiraConstants.JIRA_ISSUE_PROPERTY_KEY, properties);
    }

    private void appendPropertySearchString(StringBuilder jqlBuilder, String key, String value) {
        if (StringUtils.isNotBlank(value)) {
            jqlBuilder.append(SEARCH_CONJUNCTION);
            jqlBuilder.append(StringUtils.SPACE);
            jqlBuilder.append(createPropertySearchString(key, value));
            jqlBuilder.append(StringUtils.SPACE);
        }
    }

    private String createPropertySearchString(String key, String value) {
        final String propertySearchFormat = "issue.property[%s].%s = '%s'";
        final String escapedValue = escapeReservedCharacters(value);
        return String.format(propertySearchFormat, JiraConstants.JIRA_ISSUE_PROPERTY_KEY, key, escapedValue);
    }

    // TODO move this code to int-jira-common
    private String escapeReservedCharacters(String originalString) {
        StringBuilder stringBuilder = new StringBuilder();
        for (Character character : originalString.toCharArray()) {
            if (ADVANCED_SEARCH_RESERVED_CHARACTERS.contains(character)) {
                stringBuilder.append("\\\\" + character);
            } else {
                stringBuilder.append(character);
            }
        }
        return stringBuilder.toString();
    }

}
