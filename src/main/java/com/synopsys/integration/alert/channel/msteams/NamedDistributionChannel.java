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
package com.synopsys.integration.alert.channel.msteams;

import com.google.gson.Gson;
import com.synopsys.integration.alert.common.channel.DistributionChannel;
import com.synopsys.integration.alert.common.event.DistributionEvent;
import com.synopsys.integration.alert.common.persistence.accessor.AuditUtility;
import com.synopsys.integration.exception.IntegrationException;

public abstract class NamedDistributionChannel extends DistributionChannel {
    private String channelName;

    public NamedDistributionChannel(String channelName, Gson gson, AuditUtility auditUtility) {
        super(gson, auditUtility);
        this.channelName = channelName;
    }

    @Override
    public String sendMessage(final DistributionEvent event) throws IntegrationException {
        distributeMessage(event);
        return String.format("Successfully sent %s message.", channelName);
    }

    @Override
    public String getDestinationName() {
        return channelName;
    }

    public abstract void distributeMessage(DistributionEvent event) throws IntegrationException;

}
