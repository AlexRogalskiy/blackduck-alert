/**
 * blackduck-alert
 *
 * Copyright (C) 2019 Black Duck Software, Inc.
 * http://www.blackducksoftware.com/
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
package com.synopsys.integration.alert.web.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;

import com.synopsys.integration.alert.web.model.ResponseBodyBuilder;

@Component
public class ResponseFactory {

    public ResponseEntity<String> createResponse(final HttpStatus status, final String id, final String message) {
        final String responseBody = new ResponseBodyBuilder(id, message).build();
        return new ResponseEntity<>(responseBody, status);
    }

    public ResponseEntity<String> createResponse(final HttpStatus status, final Long id, final String message) {
        return createResponse(status, String.valueOf(id), message);
    }

    public ResponseEntity<String> createResponse(final HttpStatus status, final String message) {
        return createResponse(status, -1L, message);
    }

    public ResponseEntity<String> createNotFoundResponse(String message) {
        return createResponse(HttpStatus.NOT_FOUND, message);
    }

    public ResponseEntity<String> createCreatedResponse(String id, String message) {
        return createResponse(HttpStatus.CREATED, id, message);
    }

    public ResponseEntity<String> createAcceptedResponse(String id, String message) {
        return createResponse(HttpStatus.ACCEPTED, id, message);
    }

    public ResponseEntity<String> createOkResponse(String id, String message) {
        return createResponse(HttpStatus.OK, id, message);
    }

    public ResponseEntity<String> createGoneResponse(String id, String message) {
        return createResponse(HttpStatus.GONE, id, message);
    }

    public ResponseEntity<String> createMethodNotAllowedResponse() {
        return createResponse(HttpStatus.METHOD_NOT_ALLOWED, "This method is not allowed");
    }

    public ResponseEntity<String> createBadRequestResponse(String id, String message) {
        return createResponse(HttpStatus.BAD_REQUEST, id, message);
    }

    public ResponseEntity<String> createInternalServerErrorResponse(String id, String message) {
        return createResponse(HttpStatus.INTERNAL_SERVER_ERROR, id, message);
    }

    public ResponseEntity<String> createConflictResponse(String id, String message) {
        return createResponse(HttpStatus.CONFLICT, id, message);
    }
}
