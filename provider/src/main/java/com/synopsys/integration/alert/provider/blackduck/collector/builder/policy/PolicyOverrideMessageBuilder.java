/*
 * provider
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.provider.blackduck.collector.builder.policy;

import java.util.Collection;
import java.util.List;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.exception.AlertException;
import com.synopsys.integration.alert.common.message.model.CommonMessageData;
import com.synopsys.integration.alert.common.message.model.ComponentItem;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.alert.common.message.model.ProviderMessageContent;
import com.synopsys.integration.alert.common.persistence.model.job.DistributionJobModel;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.BlackDuckMessageBuilder;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.MessageBuilderConstants;
import com.synopsys.integration.alert.provider.blackduck.collector.builder.model.ComponentData;
import com.synopsys.integration.alert.provider.blackduck.collector.util.AlertBlackDuckService;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.api.manual.component.PolicyOverrideNotificationContent;
import com.synopsys.integration.blackduck.api.manual.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.manual.view.PolicyOverrideNotificationView;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;

@Component
public class PolicyOverrideMessageBuilder extends BlackDuckMessageBuilder<PolicyOverrideNotificationView> {
    private final Logger logger = LoggerFactory.getLogger(PolicyOverrideMessageBuilder.class);
    private final PolicyCommonBuilder policyCommonBuilder;

    @Autowired
    public PolicyOverrideMessageBuilder(PolicyCommonBuilder policyCommonBuilder) {
        super(NotificationType.POLICY_OVERRIDE);
        this.policyCommonBuilder = policyCommonBuilder;
    }

    @Override
    public List<ProviderMessageContent> buildMessageContents(CommonMessageData commonMessageData, PolicyOverrideNotificationView notificationView, BlackDuckServicesFactory blackDuckServicesFactory) {
        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        AlertBlackDuckService alertBlackDuckService = new AlertBlackDuckService(blackDuckApiClient);
        PolicyOverrideNotificationContent overrideContent = notificationView.getContent();

        String projectVersionUrl = overrideContent.getProjectVersion();
        String projectUrl = getNullableProjectUrlFromProjectVersion(projectVersionUrl, blackDuckServicesFactory.getBlackDuckApiClient(), logger::warn);
        try {
            ProviderMessageContent.Builder messageContentBuilder = new ProviderMessageContent.Builder();
            messageContentBuilder
                .applyCommonData(commonMessageData)
                .applyTopic(MessageBuilderConstants.LABEL_PROJECT_NAME, overrideContent.getProjectName(), projectUrl)
                .applySubTopic(MessageBuilderConstants.LABEL_PROJECT_VERSION_NAME, overrideContent.getProjectVersionName(), projectVersionUrl);

            List<PolicyInfo> policies = overrideContent.getPolicyInfos();
            DistributionJobModel job = commonMessageData.getJob();
            Collection<String> policyFilter = job.getPolicyFilterPolicyNames();
            List<ComponentItem> items = retrievePolicyItems(alertBlackDuckService, overrideContent, policies, commonMessageData.getNotificationId(), projectVersionUrl, policyFilter);
            messageContentBuilder.applyAllComponentItems(items);
            return List.of(messageContentBuilder.build());
        } catch (AlertException ex) {
            logger.error("Error creating policy override message.", ex);
        }

        return List.of();
    }

    private List<ComponentItem> retrievePolicyItems(AlertBlackDuckService alertBlackDuckService, PolicyOverrideNotificationContent overrideContent,
        Collection<PolicyInfo> policies, Long notificationId, String projectVersionUrl, Collection<String> policyFilter) {
        String firstName = overrideContent.getFirstName();
        String lastName = overrideContent.getLastName();

        String overrideBy = String.format("%s %s", firstName, lastName);
        LinkableItem policyOverride = new LinkableItem(MessageBuilderConstants.LABEL_POLICY_OVERRIDE_BY, overrideBy);

        String componentName = overrideContent.getComponentName();
        String componentVersionName = overrideContent.getComponentVersionName();
        ComponentData componentData = new ComponentData(componentName, componentVersionName, projectVersionUrl, ProjectVersionView.COMPONENTS_LINK);
        return policyCommonBuilder.retrievePolicyItems(getNotificationType(), alertBlackDuckService, componentData, policies, notificationId, ItemOperation.DELETE, overrideContent.getBomComponent(), List.of(policyOverride), policyFilter);
    }

}
