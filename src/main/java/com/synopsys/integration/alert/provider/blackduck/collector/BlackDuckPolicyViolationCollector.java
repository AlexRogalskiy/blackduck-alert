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
package com.synopsys.integration.alert.provider.blackduck.collector;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Component;

import com.jayway.jsonpath.TypeRef;
import com.synopsys.integration.alert.common.enumeration.ComponentItemPriority;
import com.synopsys.integration.alert.common.enumeration.ItemOperation;
import com.synopsys.integration.alert.common.message.model.ComponentItem;
import com.synopsys.integration.alert.common.message.model.LinkableItem;
import com.synopsys.integration.alert.common.rest.model.AlertNotificationWrapper;
import com.synopsys.integration.alert.common.workflow.filter.field.JsonExtractor;
import com.synopsys.integration.alert.common.workflow.filter.field.JsonField;
import com.synopsys.integration.alert.common.workflow.filter.field.JsonFieldAccessor;
import com.synopsys.integration.alert.provider.blackduck.BlackDuckProperties;
import com.synopsys.integration.alert.provider.blackduck.collector.item.BlackDuckPolicyLinkableItem;
import com.synopsys.integration.alert.provider.blackduck.descriptor.BlackDuckContent;
import com.synopsys.integration.blackduck.api.generated.enumeration.NotificationType;
import com.synopsys.integration.blackduck.api.generated.view.ComponentVersionView;
import com.synopsys.integration.blackduck.api.generated.view.ProjectVersionView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomComponentView;
import com.synopsys.integration.blackduck.api.generated.view.VersionBomPolicyRuleView;
import com.synopsys.integration.blackduck.api.generated.view.VulnerableComponentView;
import com.synopsys.integration.blackduck.api.manual.component.ComponentVersionStatus;
import com.synopsys.integration.blackduck.api.manual.component.PolicyInfo;
import com.synopsys.integration.blackduck.service.model.ProjectVersionWrapper;
import com.synopsys.integration.exception.IntegrationException;

@Component
@Scope(BeanDefinition.SCOPE_PROTOTYPE)
public class BlackDuckPolicyViolationCollector extends BlackDuckPolicyCollector {
    private final Logger logger = LoggerFactory.getLogger(getClass());

    @Autowired
    public BlackDuckPolicyViolationCollector(JsonExtractor jsonExtractor, BlackDuckProperties blackDuckProperties) {
        super(jsonExtractor, Arrays.asList(BlackDuckContent.RULE_VIOLATION, BlackDuckContent.RULE_VIOLATION_CLEARED), blackDuckProperties);
    }

    @Override
    protected Collection<ComponentItem> getComponentItems(JsonFieldAccessor jsonFieldAccessor, List<JsonField<?>> notificationFields, AlertNotificationWrapper notificationContent) {
        final ItemOperation operation = getOperationFromNotification(notificationContent);
        if (operation == null) {
            return List.of();
        }
        List<ComponentItem> items = new LinkedList<>();

        List<JsonField<PolicyInfo>> policyFields = getFieldsOfType(notificationFields, new TypeRef<PolicyInfo>() {});
        List<JsonField<ComponentVersionStatus>> componentFields = getFieldsOfType(notificationFields, new TypeRef<ComponentVersionStatus>() {});
        List<JsonField<String>> stringFields = getStringFields(notificationFields);

        Map<String, PolicyInfo> policyItems = getFieldValueObjectsByLabel(jsonFieldAccessor, policyFields, BlackDuckContent.LABEL_POLICY_INFO_LIST)
                                                  .stream()
                                                  .collect(Collectors.toMap(PolicyInfo::getPolicy, Function.identity()));
        List<ComponentVersionStatus> componentVersionStatuses = getFieldValueObjectsByLabel(jsonFieldAccessor, componentFields, BlackDuckContent.LABEL_COMPONENT_VERSION_STATUS);
        String projectVersionUrl = getFieldValueObjectsByLabel(jsonFieldAccessor, stringFields, BlackDuckContent.LABEL_PROJECT_VERSION_NAME + JsonField.LABEL_URL_SUFFIX)
                                       .stream()
                                       .findFirst()
                                       .orElse("");

        Optional<String> projectVersionComponentLink = getBlackDuckDataHelper().getProjectLink(projectVersionUrl, ProjectVersionView.COMPONENTS_LINK);

        Map<BlackDuckPolicyLinkableItem, Set<PolicyInfo>> componentToPolicyMapping = createPolicyComponentToLinkableItemMapping(componentVersionStatuses, policyItems, projectVersionComponentLink);
        for (Map.Entry<BlackDuckPolicyLinkableItem, Set<PolicyInfo>> componentToPolicyEntry : componentToPolicyMapping.entrySet()) {
            BlackDuckPolicyLinkableItem policyComponentData = componentToPolicyEntry.getKey();
            Set<PolicyInfo> policies = componentToPolicyEntry.getValue();

            for (PolicyInfo policyInfo : policies) {
                ComponentItemPriority priority = mapSeverityToPriority(policyInfo.getSeverity());

                String bomComponentUrl = null;
                ComponentVersionStatus componentVersionStatus = policyComponentData.getComponentVersionStatus();
                if (null != componentVersionStatus) {
                    bomComponentUrl = componentVersionStatus.getBomComponent();
                }

                Optional<VersionBomComponentView> optionalBomComponent = getBlackDuckDataHelper().getBomComponentView(bomComponentUrl);

                List<LinkableItem> policyLinkableItems = new ArrayList<>();
                LinkableItem policyNameItem = createPolicyNameItem(policyInfo);
                policyLinkableItems.add(policyNameItem);
                Optional<LinkableItem> optionalPolicySeverityItem = createPolicySeverityItem(policyInfo);
                optionalPolicySeverityItem.ifPresent(policyLinkableItems::add);
                optionalBomComponent.ifPresent(bomComponent -> policyLinkableItems.addAll(getBlackDuckDataHelper().getLicenseLinkableItems(bomComponent)));

                Long notificationId = notificationContent.getId();
                LinkableItem componentItem = policyComponentData.getComponentItem().orElse(null);
                Optional<LinkableItem> optionalComponentVersionItem = policyComponentData.getComponentVersion();

                Optional<ComponentItem> item = addApplicableItems(notificationId, componentItem, optionalComponentVersionItem.orElse(null), policyLinkableItems, operation, priority);
                item.ifPresent(items::add);

                if (optionalBomComponent.isPresent()) {
                    VersionBomComponentView bomComponent = optionalBomComponent.get();
                    Optional<VersionBomPolicyRuleView> optionalPolicyRule = getVersionBomPolicyRuleView(policyInfo.getPolicy(), bomComponent);
                    if (optionalPolicyRule.isPresent() && getBlackDuckDataHelper().hasVulnerabilityRule(optionalPolicyRule.get())) {
                        List<ComponentItem> vulnerabilityPolicyItems = createVulnerabilityPolicyItems(bomComponent, policyNameItem, componentItem, optionalComponentVersionItem, notificationId);
                        items.addAll(vulnerabilityPolicyItems);
                    }
                }
            }
        }
        return items;
    }

    private ItemOperation getOperationFromNotification(AlertNotificationWrapper notificationContent) {
        ItemOperation operation;
        String notificationType = notificationContent.getNotificationType();
        if (NotificationType.RULE_VIOLATION_CLEARED.name().equals(notificationType)) {
            operation = ItemOperation.DELETE;
        } else if (NotificationType.RULE_VIOLATION.name().equals(notificationType)) {
            operation = ItemOperation.ADD;
        } else {
            operation = null;
            logger.error("Unrecognized notification type: The notification type '{}' is not valid for this collector.", notificationType);
        }

        return operation;
    }

    private Map<BlackDuckPolicyLinkableItem, Set<PolicyInfo>> createPolicyComponentToLinkableItemMapping(
        Collection<ComponentVersionStatus> componentVersionStatuses, Map<String, PolicyInfo> policyItems, Optional<String> projectVersionUrl) {
        Map<BlackDuckPolicyLinkableItem, Set<PolicyInfo>> componentToPolicyMapping = new HashMap<>();
        for (ComponentVersionStatus componentVersionStatus : componentVersionStatuses) {
            String projectVersionLink = projectVersionUrl.flatMap(url -> getBlackDuckDataHelper().getProjectComponentQueryLink(url, componentVersionStatus.getComponentName())).orElse(null);
            Set<PolicyInfo> componentPolicies = getPoliciesForComponent(componentVersionStatus, policyItems);

            BlackDuckPolicyLinkableItem blackDuckPolicyLinkableItem = createBlackDuckPolicyLinkableItem(componentVersionStatus, projectVersionLink);

            if (componentToPolicyMapping.containsKey(blackDuckPolicyLinkableItem)) {
                blackDuckPolicyLinkableItem.addComponentVersionItem(componentVersionStatus.getComponentVersionName(), projectVersionLink);
                blackDuckPolicyLinkableItem.setComponentVersionStatus(componentVersionStatus);
            }
            componentToPolicyMapping.put(blackDuckPolicyLinkableItem, componentPolicies);
        }
        return componentToPolicyMapping;
    }

    private Set<PolicyInfo> getPoliciesForComponent(ComponentVersionStatus componentVersionStatus, Map<String, PolicyInfo> policyItems) {
        return componentVersionStatus.getPolicies().stream()
                   .filter(policyItems::containsKey)
                   .map(policyItems::get)
                   .collect(Collectors.toSet());
    }

    private BlackDuckPolicyLinkableItem createBlackDuckPolicyLinkableItem(ComponentVersionStatus componentVersionStatus, String projectVersionWithComponentLink) {
        BlackDuckPolicyLinkableItem blackDuckPolicyLinkableItem = new BlackDuckPolicyLinkableItem();

        String componentVersionName = componentVersionStatus.getComponentVersionName();
        if (StringUtils.isNotBlank(componentVersionName)) {
            blackDuckPolicyLinkableItem.addComponentVersionItem(componentVersionName, projectVersionWithComponentLink);
            blackDuckPolicyLinkableItem.setComponentVersionStatus(componentVersionStatus);
        }

        String componentName = componentVersionStatus.getComponentName();
        if (StringUtils.isNotBlank(componentName)) {
            String componentLink = (StringUtils.isBlank(componentVersionName)) ? projectVersionWithComponentLink : null;
            blackDuckPolicyLinkableItem.addComponentNameItem(componentName, componentLink);
        }

        return blackDuckPolicyLinkableItem;
    }

    private List<ComponentItem> createVulnerabilityPolicyItems(VersionBomComponentView bomComponent, LinkableItem policyNameItem, LinkableItem componentItem, Optional<LinkableItem> optionalComponentVersionItem, Long notificationId) {
        List<ComponentItem> vulnerabilityPolicyItems = new ArrayList<>();
        Optional<ProjectVersionWrapper> optionalProjectVersionWrapper = getBlackDuckDataHelper().getProjectVersionWrapper(bomComponent);
        if (optionalProjectVersionWrapper.isPresent()) {
            try {
                List<VulnerableComponentView> vulnerableComponentViews = getBlackDuckDataHelper().getVulnerableComponentViews(optionalProjectVersionWrapper.get(), bomComponent);
                List<LinkableItem> licenseItems = getBlackDuckDataHelper().getLicenseLinkableItems(bomComponent);
                List<ComponentItem> vulnerabilityComponentItems = createVulnerabilityComponentItems(vulnerableComponentViews, licenseItems, policyNameItem, componentItem, optionalComponentVersionItem, notificationId);
                vulnerabilityPolicyItems.addAll(vulnerabilityComponentItems);

                ComponentVersionView componentVersionView = getBlackDuckService().getResponse(bomComponent.getComponentVersion(), ComponentVersionView.class);
                Optional<ComponentItem> remediationComponentItem = createRemediationComponentItem(componentVersionView, CATEGORY_TYPE, componentItem, optionalComponentVersionItem, Set.of(policyNameItem), notificationId);
                remediationComponentItem.ifPresent(vulnerabilityPolicyItems::add);
            } catch (IntegrationException e) {
                logger.debug("Could not get the project/version. Skipping vulnerability info for this policy: {}. Exception: {}", policyNameItem, e);
            }
        }
        return vulnerabilityPolicyItems;
    }

    private Optional<VersionBomPolicyRuleView> getVersionBomPolicyRuleView(String policyRuleUrl, VersionBomComponentView bomComponent) {
        return getBlackDuckDataHelper().getPolicyRulesFromComponent(bomComponent)
                   .stream()
                   .filter(ruleView -> ruleView.getHref().filter(href -> href.equals(policyRuleUrl)).isPresent())
                   .findFirst();
    }

}
