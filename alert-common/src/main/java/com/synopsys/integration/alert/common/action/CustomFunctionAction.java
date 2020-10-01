/**
 * alert-common
 *
 * Copyright (c) 2020 Synopsys, Inc.
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
package com.synopsys.integration.alert.common.action;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import com.github.jsonldjava.shaded.com.google.common.base.Predicate;
import com.synopsys.integration.alert.common.descriptor.DescriptorProcessor;
import com.synopsys.integration.alert.common.descriptor.config.field.ConfigField;
import com.synopsys.integration.alert.common.descriptor.config.field.errors.AlertFieldStatus;
import com.synopsys.integration.alert.common.descriptor.config.field.errors.FieldStatusSeverity;
import com.synopsys.integration.alert.common.descriptor.config.field.validation.FieldValidationUtility;
import com.synopsys.integration.alert.common.rest.HttpServletContentWrapper;
import com.synopsys.integration.alert.common.rest.ResponseFactory;
import com.synopsys.integration.alert.common.rest.model.FieldModel;
import com.synopsys.integration.alert.common.security.authorization.AuthorizationManager;
import com.synopsys.integration.alert.common.util.DataStructureUtils;
import com.synopsys.integration.exception.IntegrationException;

public abstract class CustomFunctionAction<T> {
    public static final String API_FUNCTION_URL = "/api/function";

    private final String fieldKey;
    private final AuthorizationManager authorizationManager;
    private final DescriptorProcessor descriptorProcessor;
    private final FieldValidationUtility fieldValidationUtility;

    public CustomFunctionAction(String fieldKey, AuthorizationManager authorizationManager, DescriptorProcessor descriptorProcessor, FieldValidationUtility fieldValidationUtility) {
        this.fieldKey = fieldKey;
        this.authorizationManager = authorizationManager;
        this.descriptorProcessor = descriptorProcessor;
        this.fieldValidationUtility = fieldValidationUtility;
    }

    public ActionResponse<T> createResponse(FieldModel fieldModel, HttpServletContentWrapper servletContentWrapper) {
        try {
            if (!isAllowed(fieldModel)) {
                return new ActionResponse<>(HttpStatus.FORBIDDEN, ResponseFactory.UNAUTHORIZED_REQUEST_MESSAGE);
            }

            Optional<ActionResponse<T>> optionalValidationError = validateRelatedFieldsAndCreateErrorResponseIfNecessary(fieldModel);
            if (optionalValidationError.isPresent()) {
                return optionalValidationError.get();
            }

            return createActionResponse(fieldModel, servletContentWrapper);
        } catch (ResponseStatusException e) {
            return new ActionResponse<>(e.getStatus(), e.getReason());
        } catch (Exception e) {
            return createErrorResponse(e);
        }
    }

    public abstract ActionResponse<T> createActionResponse(FieldModel fieldModel, HttpServletContentWrapper servletContentWrapper) throws IntegrationException;

    protected boolean isAllowed(FieldModel fieldModel) {
        return authorizationManager.hasExecutePermission(fieldModel.getContext(), fieldModel.getDescriptorName());
    }

    // TODO consider making custom-endpoints declaring a validation endpoint that must be checked first by the UI
    private Optional<ActionResponse<T>> validateRelatedFieldsAndCreateErrorResponseIfNecessary(FieldModel fieldModel) {
        List<ConfigField> fields = descriptorProcessor.retrieveUIConfigFields(fieldModel.getContext(), fieldModel.getDescriptorName());
        Map<String, ConfigField> configFields = DataStructureUtils.mapToValues(fields, ConfigField::getKey);

        ConfigField configField = configFields.get(fieldKey);
        List<AlertFieldStatus> fieldStatuses = fieldValidationUtility.validateRelatedFields(configField, configFields, fieldModel);

        Predicate<AlertFieldStatus> hasErrorSeverity = status -> FieldStatusSeverity.ERROR.equals(status.getSeverity());
        boolean hasErrors = fieldStatuses
                                .stream()
                                .anyMatch(hasErrorSeverity);
        if (hasErrors) {
            String errorMessages = fieldStatuses
                                       .stream()
                                       .filter(hasErrorSeverity)
                                       .map(status -> createFieldStatusErrorMessage(configFields, status))
                                       .collect(Collectors.joining(", "));
            ActionResponse<T> errorActionResponse = new ActionResponse<>(HttpStatus.BAD_REQUEST, String.format("There were errors with the required fields of this action: %s", errorMessages));
            return Optional.of(errorActionResponse);
        }
        return Optional.empty();
    }

    private ActionResponse<T> createErrorResponse(Exception e) {
        return new ActionResponse<>(HttpStatus.INTERNAL_SERVER_ERROR, String.format("An internal issue occurred while trying to retrieve your data: %s", e.getMessage()));
    }

    private String createFieldStatusErrorMessage(Map<String, ConfigField> configFields, AlertFieldStatus fieldStatus) {
        ConfigField relatedField = configFields.get(fieldStatus.getFieldName());
        return String.format("The related field '%s' had an error: %s", relatedField.getLabel(), fieldStatus.getFieldMessage());
    }

}
