/*
 * alert-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.common.channel.issuetracker.config;

import java.util.Optional;

import com.synopsys.integration.alert.common.rest.model.AlertSerializableModel;

public class IssueConfig extends AlertSerializableModel {
    // TODO figure out if we need all of these fields for projects
    private String projectName;
    private String projectKey;
    private String projectId;
    private String issueCreator;
    private String issueType;
    private boolean commentOnIssues;
    private String resolveTransition;
    private String openTransition;

    /* package private */ IssueConfig() {
        // For serialization
    }

    public IssueConfig(
        String projectName
        , String projectKey
        , String projectId
        , String issueCreator
        , String issueType
        , boolean commentOnIssues
        , String resolveTransition
        , String openTransition
    ) {
        this.projectName = projectName;
        this.projectKey = projectKey;
        this.projectId = projectId;
        this.issueCreator = issueCreator;
        this.issueType = issueType;
        this.commentOnIssues = commentOnIssues;
        this.resolveTransition = resolveTransition;
        this.openTransition = openTransition;
    }

    public String getProjectName() {
        return projectName;
    }

    public void setProjectName(String projectName) {
        this.projectName = projectName;
    }

    public String getProjectKey() {
        return projectKey;
    }

    public void setProjectKey(String projectKey) {
        this.projectKey = projectKey;
    }

    public String getProjectId() {
        return projectId;
    }

    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    public String getIssueCreator() {
        return issueCreator;
    }

    public void setIssueCreator(String issueCreator) {
        this.issueCreator = issueCreator;
    }

    public String getIssueType() {
        return issueType;
    }

    public void setIssueType(String issueType) {
        this.issueType = issueType;
    }

    public boolean getCommentOnIssues() {
        return commentOnIssues;
    }

    public void setCommentOnIssues(boolean commentOnIssues) {
        this.commentOnIssues = commentOnIssues;
    }

    public Optional<String> getResolveTransition() {
        return Optional.ofNullable(resolveTransition);
    }

    public void setResolveTransition(String resolveTransition) {
        this.resolveTransition = resolveTransition;
    }

    public Optional<String> getOpenTransition() {
        return Optional.ofNullable(openTransition);
    }

    public void setOpenTransition(String openTransition) {
        this.openTransition = openTransition;
    }

}
