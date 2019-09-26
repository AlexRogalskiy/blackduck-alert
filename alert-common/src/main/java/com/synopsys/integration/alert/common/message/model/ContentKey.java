/**
 * alert-common
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
package com.synopsys.integration.alert.common.message.model;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.rest.model.AlertSerializableModel;

public class ContentKey extends AlertSerializableModel {
    private static final String KEY_SEPARATOR = "_";
    private String providerName;
    private String topicName;
    private String topicValue;
    private String subTopicName;
    private String subTopicValue;
    private ItemOperation action;

    private String value;

    public ContentKey(String providerName, String topicName, String topicValue, String subTopicName, String subTopicValue, ItemOperation action) {
        this.providerName = providerName;
        this.topicName = topicName;
        this.topicValue = topicValue;
        this.subTopicName = subTopicName;
        this.subTopicValue = subTopicValue;
        this.action = action;
        this.value = generateContentKey(providerName, topicName, topicValue, subTopicName, subTopicValue, action);
    }

    public static final ContentKey of(String providerName, String topicName, String topicValue, String subTopicName, String subTopicValue, ItemOperation action) {
        return new ContentKey(providerName, topicName, topicValue, subTopicName, subTopicValue, action);
    }

    private String generateContentKey(String providerName, String topicName, String topicValue, String subTopicName, String subTopicValue, ItemOperation action) {
        final List<String> keyParts = new ArrayList<>(6);
        keyParts.add(providerName);
        keyParts.add(topicName);
        keyParts.add(topicValue);
        if (StringUtils.isNotBlank(subTopicName)) {
            keyParts.add(subTopicName);
            keyParts.add(subTopicValue);
        }
        if (null != action) {
            keyParts.add(action.name());
        }
        return StringUtils.join(keyParts, KEY_SEPARATOR);
    }

    public String getProviderName() {
        return providerName;
    }

    public String getTopicName() {
        return topicName;
    }

    public String getTopicValue() {
        return topicValue;
    }

    public String getSubTopicName() {
        return subTopicName;
    }

    public String getSubTopicValue() {
        return subTopicValue;
    }

    public String getValue() {
        return value;
    }
}
