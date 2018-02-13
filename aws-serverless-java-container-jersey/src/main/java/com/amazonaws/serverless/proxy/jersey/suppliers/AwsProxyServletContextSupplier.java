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

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.Context;

import java.util.function.Supplier;

import static com.amazonaws.serverless.proxy.jersey.JerseyHandlerFilter.JERSEY_SERVLET_REQUEST_PROPERTY;


/**
 * Implementation of Jersey's <code>Factory</code> object for <code>ServletContext</code> objects. This can be used
 * by Jersey to generate a Servlet context given an <code>AwsProxyRequest</code> event.
 *
 * <pre>
 * <code>
 *     ResourceConfig app = new ResourceConfig().packages("my.app.package")
 *         .register(new AbstractBinder() {
 *             {@literal @}Override
 *             protected void configure() {
 *                 bindFactory(AwsProxyServletContextSupplier.class)
 *                     .to(ServletContext.class)
 *                     .in(RequestScoped.class);
 *            }
 *       });
 * </code>
 * </pre>
 */
public class AwsProxyServletContextSupplier implements Supplier<ServletContext> {
    @Context ContainerRequest currentRequest;

    @Override
    public ServletContext get() {
        return getServletContext();
    }

    private ServletContext getServletContext() {
        HttpServletRequest req = (HttpServletRequest)currentRequest.getProperty(JERSEY_SERVLET_REQUEST_PROPERTY);

        if (req == null) {
            throw new InternalServerErrorException("Could not find servlet request in context");
        }

        ServletContext ctx = req.getServletContext();
        return ctx;
    }
}
