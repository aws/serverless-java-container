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
package com.amazonaws.serverless.proxy.jersey.suppliers;


import org.glassfish.jersey.server.ContainerRequest;

import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.Context;

import java.util.function.Supplier;

import static com.amazonaws.serverless.proxy.jersey.JerseyHandlerFilter.JERSEY_SERVLET_RESPONSE_PROPERTY;


/**
 * Implementation of Jersey's <code>Factory</code> object for <code>HttpServletResponse</code> objects. This can be used
 * to write data directly to the servlet response for the method, without using Jersey's <code>ContainerResponse</code>
 *
 * <pre>
 * <code>
 *     ResourceConfig app = new ResourceConfig().packages("my.app.package")
 *         .register(new AbstractBinder() {
 *             {@literal @}Override
 *             protected void configure() {
 *                 bindFactory(AwsProxyServletResponseSupplier.class)
 *                     .to(HttpServletResponse.class)
 *                     .in(RequestScoped.class);
 *            }
 *       });
 * </code>
 * </pre>
 */
public class AwsProxyServletResponseSupplier implements Supplier<HttpServletResponse> {

    @Context ContainerRequest currentRequest;

    //-------------------------------------------------------------
    // Implementation - Factory
    //-------------------------------------------------------------

    @Override
    public HttpServletResponse get() {
        return getServletResponse();
    }

    private HttpServletResponse getServletResponse() {
        return (HttpServletResponse)currentRequest.getProperty(JERSEY_SERVLET_RESPONSE_PROPERTY);
    }
}
