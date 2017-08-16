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
package com.amazonaws.serverless.proxy.jersey;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import javax.ws.rs.core.HttpHeaders;

import java.io.ByteArrayOutputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * This object receives the <code>ContainerResponse</code> instance from the Jersey application and writes it to an
 * <code>AwsProxyResponse</code> object. The response object is passed in the constructor alongside an <code>ExceptionHandler</code>
 * instance.
 */
class JerseyResponseWriter
        implements ContainerResponseWriter {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private CountDownLatch responseMutex;
    private Map<String, String> headers;
    private int statusCode;
    private ByteArrayOutputStream responseBody;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Creates a new response writer.
     * @param latch The latch object is used to synchronize the response request handling and response generation for
     *              AWS Lambda
     */
    JerseyResponseWriter(CountDownLatch latch) {
        this.responseMutex = latch;
    }


    //-------------------------------------------------------------
    // Implementation - ContainerResponseWriter
    //-------------------------------------------------------------

    /**
     * Writes the response status code and headers, returns an OutputStream that the Jersey application can write to.
     * @param contentLength The content length for the body
     * @param containerResponse The response object from the Jersey app
     * @return An OutputStream for Jersey to write the response body to
     * @throws ContainerException default Jersey declaration
     */
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse containerResponse)
            throws ContainerException {
        statusCode = containerResponse.getStatusInfo().getStatusCode();

        if (headers == null) {
            headers = new HashMap<>();
        }

        for (final Map.Entry<String, List<String>> e : containerResponse.getStringHeaders().entrySet()) {
            for (final String value : e.getValue()) {
                // special case for set cookies
                // RFC 2109 allows for a comma separated list of cookies in one Set-Cookie header: https://tools.ietf.org/html/rfc2109
                if (e.getKey().equals(HttpHeaders.SET_COOKIE)) {
                    if (headers.containsKey(e.getKey()) && LambdaContainerHandler.getContainerConfig().isConsolidateSetCookieHeaders()) {
                        headers.put(e.getKey(), headers.get(e.getKey()) + ", " + value);
                    } else {
                        headers.put(e.getKey(), containerResponse.getStringHeaders().getFirst(e.getKey()));
                        break;
                    }
                } else {
                    headers.put(e.getKey(), value);
                }
            }
        }

        responseBody = new ByteArrayOutputStream();

        return responseBody;
    }


    public boolean suspend(long l, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
        return false;
    }


    public void setSuspendTimeout(long l, TimeUnit timeUnit) throws IllegalStateException {
    }


    public void commit() {
        responseMutex.countDown();
    }


    public void failure(Throwable throwable) {
        responseMutex.countDown();
    }


    public boolean enableResponseBuffering() {
        return false;
    }


    //-------------------------------------------------------------
    // Methods - Package
    //-------------------------------------------------------------

    Map<String, String> getHeaders() {
        return headers;
    }


    int getStatusCode() {
        return statusCode;
    }


    ByteArrayOutputStream getResponseBody() {
        return responseBody;
    }
}
