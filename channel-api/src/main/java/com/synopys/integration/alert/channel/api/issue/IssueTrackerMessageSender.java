/*
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
package com.synopys.integration.alert.channel.api.issue;

import java.io.Serializable;
import java.util.LinkedList;
import java.util.List;

import com.synopsys.integration.alert.common.channel.issuetracker.message.IssueTrackerIssueResponseModel;
import com.synopsys.integration.alert.common.channel.issuetracker.message.IssueTrackerResponse;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopys.integration.alert.channel.api.issue.model.IssueCommentModel;
import com.synopys.integration.alert.channel.api.issue.model.IssueCreationModel;
import com.synopys.integration.alert.channel.api.issue.model.IssueTrackerModelHolder;
import com.synopys.integration.alert.channel.api.issue.model.IssueTransitionModel;

// TODO consider renaming this
public abstract class IssueTrackerMessageSender<T extends Serializable> {
    public final IssueTrackerResponse sendMessages(List<IssueTrackerModelHolder<T>> channelMessages) throws AlertException {
        List<IssueTrackerIssueResponseModel> responses = new LinkedList<>();
        for (IssueTrackerModelHolder<T> channelMessage : channelMessages) {
            List<IssueTrackerIssueResponseModel> creationResponses = createIssues(channelMessage.getIssueCreationModels());
            responses.addAll(creationResponses);

            List<IssueTrackerIssueResponseModel> transitionResponses = transitionIssues(channelMessage.getIssueTransitionModels());
            responses.addAll(transitionResponses);

            List<IssueTrackerIssueResponseModel> commentResponses = commentOnIssues(channelMessage.getIssueCommentModels());
            responses.addAll(commentResponses);
        }
        return new IssueTrackerResponse("Success", responses);
    }

    protected abstract List<IssueTrackerIssueResponseModel> createIssues(List<IssueCreationModel> issueCreationModels) throws AlertException;

    protected abstract List<IssueTrackerIssueResponseModel> transitionIssues(List<IssueTransitionModel<T>> issueTransitionModels) throws AlertException;

    protected abstract List<IssueTrackerIssueResponseModel> commentOnIssues(List<IssueCommentModel<T>> issueCommentModels) throws AlertException;

}
