/**
 * channel-api
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
package com.synopys.integration.alert.channel.api.convert;

public abstract class ChannelMessageFormatter {
    public static final String DEFAULT_SECTION_SEPARATOR = "---------------------------------------";
    public static final String DEFAULT_NON_BREAKING_SPACE = " ";

    private final int maxMessageLength;
    private final String lineSeparator;
    private final String sectionSeparator;
    private final String nonBreakingSpace;

    public ChannelMessageFormatter(int maxMessageLength, String lineSeparator) {
        this(maxMessageLength, lineSeparator, DEFAULT_SECTION_SEPARATOR, DEFAULT_NON_BREAKING_SPACE);
    }

    public ChannelMessageFormatter(int maxMessageLength, String lineSeparator, String sectionSeparator, String nonBreakingSpace) {
        this.maxMessageLength = maxMessageLength;
        this.lineSeparator = lineSeparator;
        this.sectionSeparator = sectionSeparator;
        this.nonBreakingSpace = nonBreakingSpace;
    }

    public int getMaxMessageLength() {
        return maxMessageLength;
    }

    public String getLineSeparator() {
        return lineSeparator;
    }

    public String getSectionSeparator() {
        return sectionSeparator;
    }

    public String getNonBreakingSpace() {
        return nonBreakingSpace;
    }

    public abstract String encode(String txt);

    public abstract String emphasize(String txt);

    protected abstract String createLink(String txt, String url);

}