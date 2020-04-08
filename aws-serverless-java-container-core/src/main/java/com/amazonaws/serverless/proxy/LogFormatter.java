/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import javax.ws.rs.core.SecurityContext;

/**
 * Implementations of the log formatter interface are used by {@link com.amazonaws.serverless.proxy.internal.LambdaContainerHandler} class to log each request
 * processed in the container. You can set the log formatter using the {@link com.amazonaws.serverless.proxy.internal.LambdaContainerHandler#setLogFormatter(LogFormatter)}
 * method. The servlet implementation of the container ({@link com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler} includes a
 * default log formatter that produces Apache combined logs. {@link com.amazonaws.serverless.proxy.internal.servlet.ApacheCombinedServletLogFormatter}.
 * @param <ContainerRequestType> The request type used by the underlying framework
 * @param <ContainerResponseType> The response type produced by the underlying framework
 */
public interface LogFormatter<ContainerRequestType, ContainerResponseType> {
    /**
     * The format method is called by the container handler to produce the log line that should be written to the logs.
     * @param req The incoming request
     * @param res The completed response
     * @param ctx The security context produced based on the request
     * @return The log line
     */
    String format(ContainerRequestType req, ContainerResponseType res, SecurityContext ctx);
}
