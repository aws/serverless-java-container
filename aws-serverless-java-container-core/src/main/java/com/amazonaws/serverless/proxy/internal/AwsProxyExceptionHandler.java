/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.model.ErrorModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of the <code>ExceptionHandler</code> object that returns AwsProxyResponse objects.
 *
 * Returns application/json messages with a status code of 500 when the RequestReader failed to read the incoming event.
 * For all other exceptions returns a 502. Responses are populated with a JSON object containing a message property.
 *
 * @see com.amazonaws.serverless.proxy.internal.ExceptionHandler
 */
public class AwsProxyExceptionHandler
        implements ExceptionHandler<AwsProxyResponse> {

    private Logger log = LoggerFactory.getLogger(AwsProxyExceptionHandler.class);

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    static final String GATEWAY_TIMEOUT_ERROR = "Gateway timeout";


    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static Map<String, String> headers = new HashMap<>();
    private static ObjectMapper objectMapper = new ObjectMapper();


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    static {
        headers.put(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }


    //-------------------------------------------------------------
    // Implementation - ExceptionHandler
    //-------------------------------------------------------------


    @Override
    public AwsProxyResponse handle(Throwable ex) {
        log.error("Called exception handler for:", ex);
        if (ex instanceof InvalidRequestEventException) {
            return new AwsProxyResponse(500, headers, getErrorJson(INTERNAL_SERVER_ERROR));
        } else {
            return new AwsProxyResponse(502, headers, getErrorJson(GATEWAY_TIMEOUT_ERROR));
        }
    }


    @Override
    public void handle(Throwable ex, OutputStream stream) throws IOException {
        AwsProxyResponse response = handle(ex);

        objectMapper.writeValue(stream, response);
    }


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    String getErrorJson(String message) {
        try {
            return objectMapper.writeValueAsString(new ErrorModel(message));
        } catch (JsonProcessingException e) {
            log.error("Could not produce error JSON", e);
            return "{ \"message\": \"" + message + "\" }";
        }
    }
}
