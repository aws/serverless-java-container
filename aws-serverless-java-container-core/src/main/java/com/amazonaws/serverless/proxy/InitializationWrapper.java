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

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;

import java.util.concurrent.CountDownLatch;

/**
 * This class is in charge of initializing a {@link LambdaContainerHandler}.
 * In most cases, this means calling the {@link LambdaContainerHandler#initialize()} method. Some implementations may
 * require additional initialization steps, in this case implementations should provide their own
 * <code>InitializationWrapper</code>. This library includes an async implementation of this class
 * {@link AsyncInitializationWrapper} for frameworks that are likely to take longer than 10 seconds to start.
 */
public class InitializationWrapper {
    /**
     * This is the main entry point. Container handler builder and the static <code>getAwsProxyHandler()</code> methods
     * of the various implementations will call this to initialize the underlying framework
     * @param handler The container handler to be initializer
     * @throws ContainerInitializationException If anything goes wrong during container initialization.
     */
    public void start(LambdaContainerHandler handler) throws ContainerInitializationException {
        handler.initialize();
    }

    /**
     * Asynchronous implementations of the framework should return a latch that the container handler can use to decide
     * whether it can start handling events. Synchronous implementations of this interface should return <code>null</code>.
     * @return An initialized latch if the underlying container is starting in a separate thread, null otherwise.
     */
    public CountDownLatch getInitializationLatch() {
        return null;
    }
}
