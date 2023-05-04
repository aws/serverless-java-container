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

import com.amazonaws.serverless.proxy.internal.jaxrs.AwsHttpApiV2SecurityContext;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.Context;

import jakarta.ws.rs.core.SecurityContext;

public class AwsHttpApiV2SecurityContextWriter implements SecurityContextWriter<APIGatewayV2HTTPEvent> {
    @Override
    public SecurityContext writeSecurityContext(APIGatewayV2HTTPEvent event, Context lambdaContext) {
        return new AwsHttpApiV2SecurityContext(lambdaContext, event);
    }
}
