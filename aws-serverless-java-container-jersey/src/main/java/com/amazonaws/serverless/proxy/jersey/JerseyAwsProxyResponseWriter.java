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


import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

import java.util.Base64;


/**
 * Transforms the data from a JerseyResponseWriter object into a valid AwsProxyResponse object.
 *
 * @see com.amazonaws.serverless.proxy.jersey.JerseyResponseWriter
 * @see AwsProxyResponse
 */
public class JerseyAwsProxyResponseWriter extends ResponseWriter<JerseyResponseWriter, AwsProxyResponse> {

    //-------------------------------------------------------------
    // Methods - Implementation
    //-------------------------------------------------------------

    /**
     * Reads the data from the JerseyResponseWriter object and creates an AwsProxyResponse object
     *
     * @param containerResponse The container response or response reader object
     * @param lambdaContext The context for the Lambda function execution
     * @return An initialized AwsProxyResponse object
     * @throws InvalidResponseObjectException When the library fails to read the JerseyResponseWriter object
     */
    @Override
    public AwsProxyResponse writeResponse(JerseyResponseWriter containerResponse, Context lambdaContext)
            throws InvalidResponseObjectException {
        try {
            AwsProxyResponse response = new AwsProxyResponse();
            response.setStatusCode(containerResponse.getStatusCode());

            if (containerResponse.getHeaders() != null && containerResponse.getHeaders().size() > 0) {
                response.setHeaders(containerResponse.getHeaders());
            }

            if (containerResponse.getResponseBody() != null) {
                String responseString;

                if (isValidUtf8(containerResponse.getResponseBody().toByteArray())) {
                    responseString = new String(containerResponse.getResponseBody().toByteArray());
                } else {
                    responseString = Base64.getMimeEncoder().encodeToString(containerResponse.getResponseBody().toByteArray());
                    response.setBase64Encoded(true);
                }

                response.setBody(responseString);
            }

            return response;
        } catch (Exception ex) {
            throw new InvalidResponseObjectException(ex.getMessage(), ex);
        }
    }
}
