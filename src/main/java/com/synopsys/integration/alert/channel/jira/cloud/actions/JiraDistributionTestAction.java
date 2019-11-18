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
package com.synopsys.integration.alert.channel.jira.cloud.actions;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.google.gson.Gson;
import com.synopsys.integration.alert.channel.jira.cloud.JiraChannel;
import com.synopsys.integration.alert.channel.jira.cloud.JiraCloudContextBuilder;
import com.synopsys.integration.alert.channel.jira.common.JiraMessageParser;
import com.synopsys.integration.alert.channel.jira.common.JiraTestIssueCreator;
import com.synopsys.integration.alert.common.channel.ChannelDistributionTestAction;
import com.synopsys.integration.alert.common.message.model.MessageResult;
import com.synopsys.integration.alert.common.persistence.accessor.FieldAccessor;
import com.synopsys.integration.alert.issuetracker.config.IssueTrackerContext;
import com.synopsys.integration.alert.issuetracker.jira.cloud.JiraCloudCreateIssueTestAction;
import com.synopsys.integration.alert.issuetracker.jira.cloud.JiraCloudService;
import com.synopsys.integration.alert.issuetracker.message.IssueTrackerResponse;
import com.synopsys.integration.exception.IntegrationException;

@Component
public class JiraDistributionTestAction extends ChannelDistributionTestAction {
    private final Gson gson;
    private final JiraMessageParser jiraMessageParser;

    @Autowired
    public JiraDistributionTestAction(JiraChannel jiraChannel, Gson gson, JiraMessageParser jiraMessageParser) {
        super(jiraChannel);
        this.gson = gson;
        this.jiraMessageParser = jiraMessageParser;
    }

    @Override
    public MessageResult testConfig(String jobId, String destination, FieldAccessor fieldAccessor) throws IntegrationException {
        JiraCloudContextBuilder contextBuilder = new JiraCloudContextBuilder();
        IssueTrackerContext context = contextBuilder.build(fieldAccessor);
        JiraCloudService jiraService = new JiraCloudService(gson);
        JiraTestIssueCreator issueCreator = new JiraTestIssueCreator(fieldAccessor, jiraMessageParser);
        JiraCloudCreateIssueTestAction testAction = new JiraCloudCreateIssueTestAction(jiraService, gson, issueCreator);
        IssueTrackerResponse result = testAction.testConfig(context);
        return new MessageResult(result.getStatusMessage());
    }
}
