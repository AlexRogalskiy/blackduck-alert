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

import java.util.List;

import org.apache.commons.lang3.StringUtils;

import com.synopsys.integration.alert.common.rest.model.AlertSerializableModel;

public class ContentKey extends AlertSerializableModel {
    private static final String KEY_SEPARATOR = "_";
    private String providerName;
    private String topicName;
    private String topicValue;
    private String subTopicName;
    private String subTopicValue;

    private String value;

    public ContentKey(final String providerName, final String topicName, final String topicValue, final String subTopicName, final String subTopicValue) {
        this.providerName = providerName;
        this.topicName = topicName;
        this.topicValue = topicValue;
        this.subTopicName = subTopicName;
        this.subTopicValue = subTopicValue;
        this.value = generateContentKey(providerName, topicName, topicValue, subTopicName, subTopicValue);
    }

    public static final ContentKey of(String providerName, String topicName, String topicValue, String subTopicName, String subTopicValue) {
        return new ContentKey(providerName, topicName, topicValue, subTopicName, subTopicValue);
    }

    private String generateContentKey(String providerName, String topicName, String topicValue, String subTopicName, String subTopicValue) {
        final List<String> keyParts;
        if (StringUtils.isNotBlank(subTopicName)) {
            keyParts = List.of(providerName, topicName, topicValue, subTopicName, subTopicValue);
        } else {
            keyParts = List.of(providerName, topicName, topicValue);
        }
        return String.join(KEY_SEPARATOR, keyParts);
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

    //TODO add pretty print method.
}
