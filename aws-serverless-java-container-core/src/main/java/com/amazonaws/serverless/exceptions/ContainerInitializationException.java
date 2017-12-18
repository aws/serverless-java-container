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
package com.amazonaws.serverless.exceptions;


import com.amazonaws.serverless.proxy.RequestReader;


/**
 * This exception is thrown when the ContainerHandler fails to parse a request object or input stream into the
 * object required by the Container. The exception is thrown by implementing sub-classes of <code>RequestReader</code>
 *
 * @see RequestReader
 */
public class ContainerInitializationException extends Exception {
    public ContainerInitializationException(String message, Exception e) {
        super(message, e);
    }
}
