/*
 * alert-common
 *
 * Copyright (c) 2021 Synopsys, Inc.
 *
 * Use subject to the terms and conditions of the Synopsys End User Software License and Maintenance Agreement. All rights reserved worldwide.
 */
package com.synopsys.integration.alert.common.rest.model;

import java.io.Serializable;
import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.synopsys.integration.alert.api.common.model.AlertSerializableModel;

import net.minidev.json.annotate.JsonIgnore;

public class AlertPagedModel<M extends AlertSerializableModel> extends AlertPagedDetails<M> implements Serializable {
    public static final String DEFAULT_PAGE_NUMBER_STRING = "0";
    public static final String DEFAULT_PAGE_SIZE_STRING = "10";
    public static final Integer DEFAULT_PAGE_NUMBER = Integer.valueOf(DEFAULT_PAGE_NUMBER_STRING);
    public static final Integer DEFAULT_PAGE_SIZE = Integer.valueOf(DEFAULT_PAGE_SIZE_STRING);

    public AlertPagedModel(int totalPages, int currentPage, int pageSize, List<M> models) {
        super(totalPages, currentPage, pageSize, models);
    }

    @Override
    @JsonIgnore
    public List<M> getModels() {
        return super.getModels();
    }

    @JsonIgnore
    public <T extends AlertSerializableModel> AlertPagedModel<T> transformContent(Function<M, T> transformation) {
        List<T> transformedContent = getModels().stream().map(transformation).collect(Collectors.toList());
        return new AlertPagedModel<>(getTotalPages(), getCurrentPage(), getPageSize(), transformedContent);
    }

}
