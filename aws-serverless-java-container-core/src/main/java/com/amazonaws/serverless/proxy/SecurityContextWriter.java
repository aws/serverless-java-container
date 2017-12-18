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

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import javax.ws.rs.core.SecurityContext;

/**
 * This object is used by the container implementation to generated a Jax-Rs <code>SecurityContext</code> object from the
 * incoming AWS Lambda event.
 * @param <RequestType> The AWS Lambda event type
 */
public interface SecurityContextWriter<RequestType> {
    /**
     * Called by the container implementation to generate a <code>SecurityContext</code> given an incoming event. The
     * library includes a default implementation that reads from the AWS_PROXY integration events.
     *
     * @see AwsProxySecurityContextWriter
     * @see AwsProxyRequest
     *
     * @param event The incoming Lambda event
     * @param lambdaContext The context for the AWS Lambda function
     * @return A populated SecurityContext object
     */
    SecurityContext writeSecurityContext(final RequestType event, final Context lambdaContext);
}
