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
package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Base64;

/**
 * Creates an <code>AwsProxyResponse</code> object given an <code>AwsHttpServletResponse</code> object. If the
 * response is not populated with a status code we infer a default 200 status code.
 */
public class AwsProxyHttpServletResponseWriter extends ResponseWriter<AwsHttpServletResponse, AwsProxyResponse> {

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    @Override
    public AwsProxyResponse writeResponse(AwsHttpServletResponse containerResponse, Context lambdaContext)
            throws InvalidResponseObjectException {
        AwsProxyResponse awsProxyResponse = new AwsProxyResponse();
        if (containerResponse.getAwsResponseBodyString() != null) {
            String responseString;

            if (isValidUtf8(containerResponse.getAwsResponseBodyBytes())) {
                responseString = containerResponse.getAwsResponseBodyString();
            } else {
                responseString = Base64.getMimeEncoder().encodeToString(containerResponse.getAwsResponseBodyBytes());
                awsProxyResponse.setBase64Encoded(true);
            }

            awsProxyResponse.setBody(responseString);
        }
        awsProxyResponse.setHeaders(containerResponse.getAwsResponseHeaders());

        if (containerResponse.getStatus() <= 0) {
            awsProxyResponse.setStatusCode(200);
        } else {
            awsProxyResponse.setStatusCode(containerResponse.getStatus());
        }

        return awsProxyResponse;
    }
}
