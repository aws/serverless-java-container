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
package com.amazonaws.serverless.proxy.internal;


import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.OutputStream;


/**
 * Implementations of this object are used by the container to transform the container's response object into a valid
 * return type for the AWS Lambda function. The <code>ContainerResponseType</code> type could be a response model object
 * or a <code>ResponseReader</code> implementation. For example, the Jersey library passes the response reader object to
 * the default implementation of this class.
 *
 * @param <ContainerResponseType> The response object expceted from the underlying container
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
    protected abstract ResponseType writeResponse(ContainerResponseType containerResponse, Context lambdaContext)
            throws InvalidResponseObjectException;
}
