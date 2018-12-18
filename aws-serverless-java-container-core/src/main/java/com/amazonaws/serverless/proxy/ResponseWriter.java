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


import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.services.lambda.runtime.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;


/**
 * Implementations of this object are used by the container to transform the container's response object into a valid
 * return type for the AWS Lambda function. The <code>ContainerResponseType</code> type could be a response model object
 * or a <code>ResponseReader</code> implementation. For example, the Jersey library passes the response reader object to
 * the default implementation of this class.
 *
 * @param <ContainerResponseType> The response object expected from the underlying container
 * @param <ResponseType> The type for the Lambda function return value
 */
public abstract class ResponseWriter<ContainerResponseType, ResponseType> {

    //-------------------------------------------------------------
    // Methods - Abstract
    //-------------------------------------------------------------

    /**
     * Writes status code, headers, and body from the container response to a Lambda return value.
     * @param containerResponse The container response or response reader object
     * @param lambdaContext The context for the Lambda function execution
     * @return A valid return value for the Lambda function
     * @throws InvalidResponseObjectException When the implementation cannot read the container response object
     */
    public abstract ResponseType writeResponse(ContainerResponseType containerResponse, Context lambdaContext)
            throws InvalidResponseObjectException;

    /**
     * Checks whether the given byte array contains a UTF-8 encoded string
     * @param input The byte[] to check against
     * @return true if the contend is valid UTF-8, false otherwise
     */
    @SuppressFBWarnings("NS_NON_SHORT_CIRCUIT")
    protected boolean isValidUtf8(final byte[] input) {
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
