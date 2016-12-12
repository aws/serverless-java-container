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
import com.amazonaws.serverless.proxy.internal.ResponseWriter;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
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
                }

                response.setBody(responseString);
            }

            return response;
        } catch (Exception ex) {
            throw new InvalidResponseObjectException(ex.getMessage(), ex);
        }
    }


    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    /**
     * Checks whether the given byte array contains a UTF-8 encoded string
     * @param input The byte[] to check against
     * @return true if the contend is valid UTF-8, false otherwise
     */
    private static boolean isValidUtf8(final byte[] input) {
        int i = 0;
        // Check for BOM
        if (input.length >= 3 && (input[0] & 0xFF) == 0xEF
                && (input[1] & 0xFF) == 0xBB & (input[2] & 0xFF) == 0xBF) {
            i = 3;
        }

        int end;
        for (int j = input.length; i < j; ++i) {
            int octet = input[i];
            if ((octet & 0x80) == 0) {
                continue; // ASCII
            }

            // Check for UTF-8 leading byte
            if ((octet & 0xE0) == 0xC0) {
                end = i + 1;
            } else if ((octet & 0xF0) == 0xE0) {
                end = i + 2;
            } else if ((octet & 0xF8) == 0xF0) {
                end = i + 3;
            } else {
                // Java only supports BMP so 3 is max
                return false;
            }

            while (i < end) {
                i++;
                octet = input[i];
                if ((octet & 0xC0) != 0x80) {
                    // Not a valid trailing byte
                    return false;
                }
            }
        }
        return true;
    }
}
