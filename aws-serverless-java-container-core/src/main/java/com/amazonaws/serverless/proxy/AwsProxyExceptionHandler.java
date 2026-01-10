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
package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.ErrorModel;
import com.amazonaws.serverless.proxy.model.Headers;

import tools.jackson.core.JacksonException;
import jakarta.ws.rs.core.Response;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.OutputStream;

/**
 * Default implementation of the <code>ExceptionHandler</code> object that returns AwsProxyResponse objects.
 *
 * Returns application/json messages with a status code of 500 when the RequestReader failed to read the incoming event
 *  or if InternalServerErrorException is thrown.
 * For all other exceptions returns a 502. Responses are populated with a JSON object containing a message property.
 *
 * @see ExceptionHandler
 */
public class AwsProxyExceptionHandler
        implements ExceptionHandler<AwsProxyResponse> {

    private Logger log = LoggerFactory.getLogger(AwsProxyExceptionHandler.class);

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    static final String INTERNAL_SERVER_ERROR = Response.Status.INTERNAL_SERVER_ERROR.getReasonPhrase();
    static final String GATEWAY_TIMEOUT_ERROR = Response.Status.GATEWAY_TIMEOUT.getReasonPhrase();


    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    protected static final Headers HEADERS = new Headers();

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    static {
        HEADERS.putSingle(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }


    //-------------------------------------------------------------
    // Implementation - ExceptionHandler
    //-------------------------------------------------------------


    @Override
    public AwsProxyResponse handle(Throwable ex) {
        log.error("Called exception handler for:", ex);

        // adding a print stack trace in case we have no appender or we are running inside SAM local, where need the
        // output to go to the stderr.
        ex.printStackTrace();
        if (ex instanceof InvalidRequestEventException || ex instanceof InternalServerErrorException) {
            return new AwsProxyResponse(500, HEADERS, getErrorJson(INTERNAL_SERVER_ERROR));
        } else {
            return new AwsProxyResponse(502, HEADERS, getErrorJson(GATEWAY_TIMEOUT_ERROR));
        }
    }


    @Override
    public void handle(Throwable ex, OutputStream stream) throws IOException {
        AwsProxyResponse response = handle(ex);

        LambdaContainerHandler.getObjectMapper().writeValue(stream, response);
    }


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    protected String getErrorJson(String message) {

        try {
            return LambdaContainerHandler.getObjectMapper().writeValueAsString(new ErrorModel(message));
        } catch (JacksonException e) {
            log.error("Could not produce error JSON", e);
            return "{ \"message\": \"" + message + "\" }";
        }
    }
}
