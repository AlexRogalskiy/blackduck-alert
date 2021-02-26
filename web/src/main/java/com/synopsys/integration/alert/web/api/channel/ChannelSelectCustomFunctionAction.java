/*
 * web
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.web.api.channel;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.action.ActionResponse;
import com.synopsys.integration.alert.common.action.CustomFunctionAction;
import com.synopsys.integration.alert.common.descriptor.Descriptor;
import com.synopsys.integration.alert.common.descriptor.DescriptorMap;
import com.synopsys.integration.alert.common.descriptor.config.field.LabelValueSelectOption;
import com.synopsys.integration.alert.common.descriptor.config.field.LabelValueSelectOptions;
import com.synopsys.integration.alert.common.descriptor.config.field.validation.FieldValidationUtility;
import com.synopsys.integration.alert.common.descriptor.config.ui.ChannelDistributionUIConfig;
import com.synopsys.integration.alert.common.enumeration.ConfigContextEnum;
import com.synopsys.integration.alert.common.enumeration.DescriptorType;
import com.synopsys.integration.alert.common.rest.HttpServletContentWrapper;
import com.synopsys.integration.alert.common.rest.model.FieldModel;
import com.synopsys.integration.alert.common.security.authorization.AuthorizationManager;

@Component
public class ChannelSelectCustomFunctionAction extends CustomFunctionAction<LabelValueSelectOptions> {
    private final DescriptorMap descriptorMap;
    private final AuthorizationManager authorizationManager;

    @Autowired
    public ChannelSelectCustomFunctionAction(DescriptorMap descriptorMap, AuthorizationManager authorizationManager, FieldValidationUtility fieldValidationUtility) {
        super(ChannelDistributionUIConfig.KEY_CHANNEL_NAME, authorizationManager, descriptorMap, fieldValidationUtility);
        this.descriptorMap = descriptorMap;
        this.authorizationManager = authorizationManager;
    }

    @Override
    public ActionResponse<LabelValueSelectOptions> createActionResponse(FieldModel fieldModel, HttpServletContentWrapper servletContentWrapper) {
        List<LabelValueSelectOption> options = descriptorMap.getDescriptorByType(DescriptorType.CHANNEL).stream()
                                                         .filter(this::hasPermission)
                                                         .map(descriptor -> descriptor.getUIConfig(ConfigContextEnum.DISTRIBUTION))
                                                         .flatMap(Optional::stream)
                                                         .map(uiConfig -> (ChannelDistributionUIConfig) uiConfig)
                                                         .map(channelDistributionUIConfig -> new LabelValueSelectOption(channelDistributionUIConfig.getLabel(), channelDistributionUIConfig.getChannelKey().getUniversalKey()))
                                                         .sorted()
                                                         .collect(Collectors.toList());
        LabelValueSelectOptions optionList = new LabelValueSelectOptions(options);
        return new ActionResponse<>(HttpStatus.OK, optionList);
    }

    private boolean hasPermission(Descriptor descriptor) {
        return authorizationManager.hasPermissions(ConfigContextEnum.DISTRIBUTION, descriptor.getDescriptorKey());
    }
}
