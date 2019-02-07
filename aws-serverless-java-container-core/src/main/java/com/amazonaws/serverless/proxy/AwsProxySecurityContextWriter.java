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

import com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import javax.ws.rs.core.SecurityContext;

/**
 * Default implementation of <code>SecurityContextWriter</code>. Creates a SecurityContext object based on an API Gateway
 * event and the Lambda context. This returns the default <code>AwsProxySecurityContext</code> instance.
 */
public class AwsProxySecurityContextWriter implements SecurityContextWriter<AwsProxyRequest> {

    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private AwsProxySecurityContext currentContext;


    //-------------------------------------------------------------
    // Implementation - SecurityContextWriter
    //-------------------------------------------------------------

    @Override
    public SecurityContext writeSecurityContext(AwsProxyRequest event, Context lambdaContext) {
       currentContext = new AwsProxySecurityContext(lambdaContext, event);

        return currentContext;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public AwsProxySecurityContext getCurrentContext() {
        return currentContext;
    }
}
