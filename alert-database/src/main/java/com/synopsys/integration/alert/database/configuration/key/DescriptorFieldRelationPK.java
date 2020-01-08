/**
 * alert-database
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
package com.synopsys.integration.alert.database.configuration.key;

import java.io.Serializable;

public class DescriptorFieldRelationPK implements Serializable {
    private Long descriptorId;
    private Long fieldId;

    public DescriptorFieldRelationPK() {
        // JPA requires default constructor definitions
    }

    public DescriptorFieldRelationPK(final Long descriptorId, final Long fieldId) {
        this.descriptorId = descriptorId;
        this.fieldId = fieldId;
    }

    public Long getDescriptorId() {
        return descriptorId;
    }

    public void setDescriptorId(final Long descriptorId) {
        this.descriptorId = descriptorId;
    }

    public Long getFieldId() {
        return fieldId;
    }

    public void setFieldId(final Long fieldId) {
        this.fieldId = fieldId;
    }
}
