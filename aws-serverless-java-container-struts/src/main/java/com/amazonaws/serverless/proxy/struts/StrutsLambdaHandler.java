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
package com.amazonaws.serverless.proxy.struts;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The lambda handler to handle the requests.
 * <p>
 * <code>
 * com.amazonaws.serverless.proxy.struts.StrutsLambdaHandler::handleRequest
 * </code>
 */
public class StrutsLambdaHandler implements RequestStreamHandler {

    private final StrutsLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponse> handler = StrutsLambdaContainerHandler
            .getAwsProxyHandler();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
