/*
 * component
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.component.settings.encryption.validator;

import java.util.HashSet;
import java.util.Set;
import java.util.function.BooleanSupplier;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.common.descriptor.config.field.errors.AlertFieldStatus;
import com.synopsys.integration.alert.common.descriptor.config.field.errors.AlertFieldStatusMessages;
import com.synopsys.integration.alert.common.enumeration.SystemMessageSeverity;
import com.synopsys.integration.alert.common.enumeration.SystemMessageType;
import com.synopsys.integration.alert.common.persistence.accessor.SystemMessageAccessor;
import com.synopsys.integration.alert.common.rest.model.ValidationResponseModel;
import com.synopsys.integration.alert.common.security.EncryptionUtility;
import com.synopsys.integration.alert.common.system.BaseSystemValidator;
import com.synopsys.integration.alert.component.settings.descriptor.SettingsDescriptor;
import com.synopsys.integration.alert.component.settings.encryption.model.SettingsEncryptionModel;

@Component
public class SettingsEncryptionValidator extends BaseSystemValidator {
    private final Logger logger = LoggerFactory.getLogger(getClass());
    private final EncryptionUtility encryptionUtility;

    @Autowired
    public SettingsEncryptionValidator(EncryptionUtility encryptionUtility, SystemMessageAccessor systemMessageAccessor) {
        super(systemMessageAccessor);
        this.encryptionUtility = encryptionUtility;
    }

    public ValidationResponseModel validate(SettingsEncryptionModel model) {
        Set<AlertFieldStatus> statuses = new HashSet<>();
        getSystemMessageAccessor().removeSystemMessagesByType(SystemMessageType.ENCRYPTION_CONFIGURATION_ERROR);

        boolean isPasswordMissing = model.getPassword().filter(StringUtils::isNotBlank).isEmpty();
        boolean isGlobalSaltMissing = model.getGlobalSalt().filter(StringUtils::isNotBlank).isEmpty();
        checkFieldInitialized(statuses, "encryptionPassword", SettingsDescriptor.FIELD_ERROR_ENCRYPTION_PWD, isPasswordMissing, encryptionUtility::isPasswordMissing);
        checkFieldInitialized(statuses, "encryptionGlobalSalt", SettingsDescriptor.FIELD_ERROR_ENCRYPTION_GLOBAL_SALT, isGlobalSaltMissing, encryptionUtility::isGlobalSaltMissing);

        if (!statuses.isEmpty()) {
            return ValidationResponseModel.fromStatusCollection(statuses);
        }

        return ValidationResponseModel.success();
    }

    private void checkFieldInitialized(Set<AlertFieldStatus> statuses, String fieldName, String fieldErrorMessage, boolean isFieldMissingFromModel, BooleanSupplier isFieldMissing) {
        boolean encryptionInitialized = encryptionUtility.isInitialized();
        if (isFieldMissingFromModel && !encryptionInitialized) {
            boolean encryptionError = addSystemMessageForError(fieldErrorMessage, SystemMessageSeverity.ERROR, SystemMessageType.ENCRYPTION_CONFIGURATION_ERROR,
                isFieldMissing.getAsBoolean());
            if (encryptionError) {
                logger.error(fieldErrorMessage);
                statuses.add(AlertFieldStatus.error(fieldName, AlertFieldStatusMessages.REQUIRED_FIELD_MISSING));
            }
        }
    }
}
