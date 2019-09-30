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


import com.amazonaws.serverless.proxy.internal.testutils.Timer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.InternalServerErrorException;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;


/**
 * This object receives the <code>ContainerResponse</code> instance from the Jersey application and writes it to an
 * <code>AwsProxyResponse</code> object. The response object is passed in the constructor alongside an <code>ExceptionHandler</code>
 * instance.
 */
class JerseyServletResponseWriter
        implements ContainerResponseWriter {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private HttpServletResponse servletResponse;
    private Logger log = LoggerFactory.getLogger(JerseyServletResponseWriter.class);
    private CountDownLatch jerseyLatch;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Creates a new response writer.
     * @param resp The current ServletResponse from the container
     */
    public JerseyServletResponseWriter(ServletResponse resp, CountDownLatch latch) {
        assert resp instanceof HttpServletResponse;
        servletResponse = (HttpServletResponse)resp;
        jerseyLatch = latch;
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
    @SuppressFBWarnings("HTTP_RESPONSE_SPLITTING") // suppress this because headers are sanitized in the setHeader method of the servlet response
    public OutputStream writeResponseStatusAndHeaders(long contentLength, ContainerResponse containerResponse)
            throws ContainerException {
        Timer.start("JERSEY_WRITE_RESPONSE");
        servletResponse.setStatus(containerResponse.getStatusInfo().getStatusCode());
        for (final Map.Entry<String, List<String>> e : containerResponse.getStringHeaders().entrySet()) {
            for (final String value : e.getValue()) {
                servletResponse.setHeader(e.getKey(), value);
            }
        }
        try {
            Timer.stop("JERSEY_WRITE_RESPONSE");
            return servletResponse.getOutputStream();
        } catch (IOException e) {
            log.error("Could not get servlet response output stream", e);
            Timer.stop("JERSEY_WRITE_RESPONSE");
            throw new InternalServerErrorException("Could not get servlet response output stream", e);
        }

    }


    public boolean suspend(long l, TimeUnit timeUnit, TimeoutHandler timeoutHandler) {
        log.debug("Suspend");
        return false;
    }


    public void setSuspendTimeout(long l, TimeUnit timeUnit) throws IllegalStateException {
        log.debug("SuspectTimeout");
    }


    public void commit() {
        try {
            log.debug("commit");
            jerseyLatch.countDown();
            servletResponse.flushBuffer();
        } catch (IOException e) {
            log.error("Could not commit response", e);
            throw new InternalServerErrorException(e);
        }
    }


    public void failure(Throwable throwable) {
        log.error("failure", throwable);
        throw new InternalServerErrorException("Jersey failed to process request", throwable);
    }


    public boolean enableResponseBuffering() {
        return false;
    }
}
