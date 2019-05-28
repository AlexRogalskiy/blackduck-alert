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
package com.synopsys.integration.alert.channel.jira.descriptor;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.channel.jira.JiraChannel;
import com.synopsys.integration.alert.common.descriptor.ChannelDescriptor;

@Component
public class JiraDescriptor extends ChannelDescriptor {
    public static final String KEY_JIRA_URL = "jira.cloud.url";
    public static final String KEY_JIRA_USERNAME = "jira.cloud.user.name";
    public static final String KEY_JIRA_ACCESS_TOKEN = "jira.cloud.access.token";

    public static final String KEY_ADD_COMMENTS = "channel.cloud.jira.add.comments";
    public static final String KEY_ISSUE_CREATOR = "channel.cloud.jira.issue.creator";
    public static final String KEY_JIRA_PROJECT_NAME = "channel.cloud.jira.project.name";

    public static final String JIRA_LABEL = "Jira Cloud";
    public static final String JIRA_URL = "jira";
    // brands are in the fab icon set use the / character to delimit the icon set.
    public static final String JIRA_ICON = "fab/jira";
    public static final String JIRA_DESCRIPTION = "This page allows you to configure the Jira Cloud instance that Alert will send issue updates to.";

    @Autowired
    public JiraDescriptor(final JiraGlobalUIConfig globalUIConfig, final JiraDistributionUIConfig distributionUIConfig) {
        super(JiraChannel.COMPONENT_NAME, distributionUIConfig, globalUIConfig);
    }
}
