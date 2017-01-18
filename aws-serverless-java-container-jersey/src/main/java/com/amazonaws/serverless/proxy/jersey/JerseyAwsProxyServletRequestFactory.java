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


import com.amazonaws.serverless.proxy.internal.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
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
 *                 bindFactory(JerseyAwsProxyServletRequestFactory.class)
 *                     .to(HttpServletRequest.class)
 *                     .in(RequestScoped.class);
 *            }
 *       });
 * </code>
 * </pre>
 */
public class JerseyAwsProxyServletRequestFactory
        implements Factory<HttpServletRequest> {

    //-------------------------------------------------------------
    // Implementation - Factory
    //-------------------------------------------------------------

    @Override
    public HttpServletRequest provide() {
        return new AwsProxyHttpServletRequest(JerseyAwsProxyRequestReader.getCurrentRequest(),
                                              JerseyAwsProxyRequestReader.getCurrentLambdaContext(),
                                              AwsProxySecurityContextWriter.getCurrentContext());
    }


    @Override
    public void dispose(HttpServletRequest httpServletRequest) {
    }
}
