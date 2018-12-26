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

import java.io.IOException;
import java.io.OutputStream;

/**
 * Implementing sub-classes of this interface are used by container objects to handle exceptions. Normally, exceptions are
 * handled by the client applications directly within the container and a valid HTTP response is expected. This handler
 * is used for exceptions thrown by the library while marshalling and unmarshalling requests and responses.
 *
 * The interface declares two methods. A typed <code>handle</code> method for requests that are being proxied using a
 * request and response type <code>LambdaContainerHandler</code>, and a stream-based
 * <code>handle</code> method for <a href="http://docs.aws.amazon.com/lambda/latest/dg/java-handler-io-type-stream.html" target="_blank">
 * Lambda's <code>RequestStreamHandler</code></a>.
 *
 * @see com.amazonaws.serverless.proxy.internal.LambdaContainerHandler
 *
 * @param <ResponseType> The type for the Lambda return value. Implementing sub-classes are required to return a valid
 *                      instance of the response type.
 */
public interface ExceptionHandler<ResponseType> {
    /**
     * The handle method is called by the container object whenever an exception occurs in the <code>proxy</code> method
     * for typed calls
     * @param ex The exception thrown by the application
     * @return A valid response object
     */
    ResponseType handle(Throwable ex);

    /**
     * This handle implementation is called whenever an exception occurs in the stream-based <code>proxy</code> method.
     * @param ex The exception thrown by the application
     * @param stream The OutputStream where the exception will be written
     * @throws IOException When the exception handler fails to write to the OutputStream
     */
    void handle(Throwable ex, OutputStream stream)
            throws IOException;
}
