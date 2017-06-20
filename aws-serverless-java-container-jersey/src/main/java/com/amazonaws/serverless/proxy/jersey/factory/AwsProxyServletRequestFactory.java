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


import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.model.ContainerConfig;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.jersey.JerseyAwsProxyRequestReader;
import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;

import org.glassfish.hk2.api.Factory;

import javax.servlet.http.HttpServletRequest;

/**
 * Implementation of Jersey's <code>Factory</code> object for <code>HttpServletRequest</code> objects. This can be used
 * by Jersey to generate a Servlet request given an <code>AwsProxyRequest</code> event.
 *
 * <pre>
 * <code>
 *     ResourceConfig app = new ResourceConfig().packages("my.app.package")
 *         .register(new AbstractBinder() {
 *             {@literal @}Override
 *             protected void configure() {
 *                 bindFactory(AwsProxyServletRequestFactory.class)
 *                     .to(HttpServletRequest.class)
 *                     .in(RequestScoped.class);
 *            }
 *       });
 * </code>
 * </pre>
 */
public class AwsProxyServletRequestFactory
        implements Factory<HttpServletRequest> {

    private static AwsProxyHttpServletRequestReader requestReader = new AwsProxyHttpServletRequestReader();

    //-------------------------------------------------------------
    // Implementation - Factory
    //-------------------------------------------------------------

    @Override
    public HttpServletRequest provide() {
        return getRequest();
    }


    @Override
    public void dispose(HttpServletRequest httpServletRequest) {
    }

    public static HttpServletRequest getRequest() {
        try {
            AwsProxyHttpServletRequest req =  requestReader.readRequest(JerseyAwsProxyRequestReader.getCurrentRequest(),
                                                                        AwsProxySecurityContextWriter.getCurrentContext(),
                                                                        JerseyAwsProxyRequestReader.getCurrentLambdaContext(),
                                                                        JerseyLambdaContainerHandler.getContainerConfig());
            req.setServletContext(new AwsServletContext(JerseyAwsProxyRequestReader.getCurrentLambdaContext(), null));
            return req;
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            return null;
        }

    }
}
