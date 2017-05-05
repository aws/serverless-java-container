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
package com.amazonaws.serverless.proxy.jersey.factory;


import org.glassfish.hk2.api.Factory;

import javax.servlet.ServletContext;


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
 *                 bindFactory(AwsProxyServletContextFactory.class)
 *                     .to(ServletContext.class)
 *                     .in(RequestScoped.class);
 *            }
 *       });
 * </code>
 * </pre>
 */
public class AwsProxyServletContextFactory implements Factory<ServletContext> {
    @Override
    public ServletContext provide() {
        return AwsProxyServletRequestFactory.getRequest().getServletContext();
    }


    @Override
    public void dispose(ServletContext servletContext) {

    }
}
