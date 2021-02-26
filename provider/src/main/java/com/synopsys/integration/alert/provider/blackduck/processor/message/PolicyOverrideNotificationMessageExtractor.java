/*
 * provider
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.provider.blackduck.processor.message;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.alert.descriptor.api.BlackDuckProviderKey;
import com.synopsys.integration.alert.processor.api.extract.model.project.BomComponentDetails;
import com.synopsys.integration.alert.processor.api.extract.model.project.ComponentConcern;
import com.synopsys.integration.alert.provider.blackduck.processor.NotificationExtractorBlackDuckServicesFactoryCache;
import com.synopsys.integration.alert.provider.blackduck.processor.message.util.BlackDuckMessageBomComponentDetailsUtils;
import com.synopsys.integration.alert.provider.blackduck.processor.message.util.BlackDuckMessageComponentConcernUtils;
import com.synopsys.integration.alert.provider.blackduck.processor.model.PolicyOverrideUniquePolicyNotificationContent;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionComponentView;
import com.synopsys.integration.blackduck.api.manual.enumeration.NotificationType;
import com.synopsys.integration.blackduck.service.BlackDuckApiClient;
import com.synopsys.integration.blackduck.service.BlackDuckServicesFactory;
import com.synopsys.integration.exception.IntegrationException;
import com.synopsys.integration.rest.HttpUrl;

@Component
public class PolicyOverrideNotificationMessageExtractor extends AbstractBlackDuckComponentConcernMessageExtractor<PolicyOverrideUniquePolicyNotificationContent> {
    @Autowired
    public PolicyOverrideNotificationMessageExtractor(BlackDuckProviderKey blackDuckProviderKey, NotificationExtractorBlackDuckServicesFactoryCache servicesFactoryCache) {
        super(NotificationType.POLICY_OVERRIDE, PolicyOverrideUniquePolicyNotificationContent.class, blackDuckProviderKey, servicesFactoryCache);
    }

    @Override
    protected List<BomComponentDetails> createBomComponentDetails(PolicyOverrideUniquePolicyNotificationContent notificationContent, BlackDuckServicesFactory blackDuckServicesFactory) throws IntegrationException {
        BlackDuckApiClient blackDuckApiClient = blackDuckServicesFactory.getBlackDuckApiClient();
        ProjectVersionComponentView bomComponent = blackDuckApiClient.getResponse(new HttpUrl(notificationContent.getBomComponent()), ProjectVersionComponentView.class);
        ComponentConcern policyConcern = BlackDuckMessageComponentConcernUtils.fromPolicyInfo(notificationContent.getPolicyInfo(), ItemOperation.DELETE);

        String overriderName = String.format("%s %s", notificationContent.getFirstName(), notificationContent.getLastName());
        LinkableItem overrider = new LinkableItem(BlackDuckMessageLabels.LABEL_OVERRIDER, overriderName);

        BomComponentDetails bomComponentDetails = BlackDuckMessageBomComponentDetailsUtils.createBomComponentDetails(bomComponent, policyConcern, List.of(overrider));
        return List.of(bomComponentDetails);
    }

}
