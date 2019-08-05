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
package com.synopsys.integration.alert.channel.jira.actions;

import java.util.Date;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.channel.jira.JiraChannel;
import com.synopsys.integration.alert.common.action.ChannelDistributionTestAction;
import com.synopsys.integration.alert.common.descriptor.config.ui.ChannelDistributionUIConfig;
import com.synopsys.integration.alert.common.descriptor.config.ui.ProviderDistributionUIConfig;
import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.event.DistributionEvent;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.message.model.ComponentItem;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.alert.common.message.model.MessageContentGroup;
import com.synopsys.integration.alert.common.message.model.ProviderMessageContent;
import com.synopsys.integration.alert.common.persistence.accessor.FieldAccessor;
import com.synopsys.integration.alert.common.rest.model.TestConfigModel;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.RestConstants;

@Component
public class JiraDistributionTestAction extends ChannelDistributionTestAction {

    @Autowired
    public JiraDistributionTestAction(final JiraChannel jiraChannel) {
        super(jiraChannel);
    }

    @Override
    public String testConfig(final TestConfigModel testConfigModel) throws IntegrationException {
        final FieldAccessor fieldAccessor = testConfigModel.getFieldAccessor();
        String configId = testConfigModel.getConfigId().orElse(null);
        String messageId = UUID.randomUUID().toString();
        final DistributionEvent createIssueEvent = createChannelTestEvent(configId, fieldAccessor, ItemOperation.ADD, messageId);
        getDistributionChannel().sendMessage(createIssueEvent);

        final DistributionEvent resolveIssueEvent = createChannelTestEvent(configId, fieldAccessor, ItemOperation.DELETE, messageId);
        getDistributionChannel().sendMessage(resolveIssueEvent);

        final DistributionEvent reOpenIssueEvent = createChannelTestEvent(configId, fieldAccessor, ItemOperation.ADD, messageId);
        return getDistributionChannel().sendMessage(reOpenIssueEvent);
    }

    public DistributionEvent createChannelTestEvent(final String configId, final FieldAccessor fieldAccessor, ItemOperation operation, String messageId) throws AlertException {
        final ProviderMessageContent messageContent = createTestNotificationContent(operation, messageId);

        final String channelName = fieldAccessor.getString(ChannelDistributionUIConfig.KEY_CHANNEL_NAME).orElse("");
        final String providerName = fieldAccessor.getString(ChannelDistributionUIConfig.KEY_PROVIDER_NAME).orElse("");
        final String formatType = fieldAccessor.getString(ProviderDistributionUIConfig.KEY_FORMAT_TYPE).orElse("");

        return new DistributionEvent(configId, channelName, RestConstants.formatDate(new Date()), providerName, formatType, MessageContentGroup.singleton(messageContent), fieldAccessor);
    }

    public ProviderMessageContent createTestNotificationContent(ItemOperation operation, String messageId) throws AlertException {
        ProviderMessageContent.Builder builder = new ProviderMessageContent.Builder();
        builder.applyProvider("Alert");
        builder.applyTopic("Test Topic", "Alert Test Message");
        builder.applySubTopic("Test SubTopic", "Test message sent by Alert");
        builder.applyComponentItem(createTestComponentItem(operation, messageId));
        return builder.build();
    }

    private ComponentItem createTestComponentItem(ItemOperation operation, String messageId) throws AlertException {
        final ComponentItem.Builder builder = new ComponentItem.Builder();
        builder.applyOperation(operation);
        builder.applyCategory("Test Category");
        builder.applyComponentData("Message ID", messageId);
        LinkableItem keyItem = new LinkableItem("Test linkable item", messageId);
        builder.applyComponentAttribute(keyItem, true);
        builder.applyNotificationId(1L);
        return builder.build();
    }
}
