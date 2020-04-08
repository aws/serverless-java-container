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
import com.amazonaws.services.lambda.runtime.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.management.ManagementFactory;
import java.time.Instant;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * An async implementation of the <code>InitializationWrapper</code> interface. This initializer calls the
 * {@link LambdaContainerHandler#initialize()} in a separate thread. Then uses a latch to wait for the maximum Lambda
 * initialization time of 10 seconds, if the <code>initialize</code> method takes longer than 10 seconds to return, the
 * {@link #start(LambdaContainerHandler)} returns control to the caller and lets the initialization thread continue in
 * the background. The {@link LambdaContainerHandler#proxy(Object, Context)} automatically waits for the latch of the
 * initializer to be released.
 *
 * The constructor of this class expects an epoch long. This is meant to be as close as possible to the time the Lambda
 * function actually started. In most cases, the first action in the constructor of the handler class should be to populate
 * this long value ({@code Instant.now().toEpochMs();}). This class uses the value to estimate how much of the init 10
 * seconds has already been used up.
 */
public class AsyncInitializationWrapper extends InitializationWrapper {
    private int INIT_GRACE_TIME_MS = 250;
    private static final int LAMBDA_MAX_INIT_TIME_MS = 10_000;

    private CountDownLatch initializationLatch;
    private long actualStartTime;
    private Logger log = LoggerFactory.getLogger(AsyncInitializationWrapper.class);

    /**
     * Creates a new instance of the async initializer.
     * @param startTime The epoch ms start time of the Lambda function, this should be measured as close as possible to
     *                  the initialization of the function.
     */
    public AsyncInitializationWrapper(long startTime) {
        actualStartTime = startTime;
    }

    /**
     * Creates a new instance of the async initializer using the actual JVM start time as the starting point to measure
     * the 10 seconds timeout.
     */
    public AsyncInitializationWrapper() {
        actualStartTime = ManagementFactory.getRuntimeMXBean().getStartTime();
        INIT_GRACE_TIME_MS = 150;
    }

    @Override
    public void start(LambdaContainerHandler handler) throws ContainerInitializationException {
        initializationLatch = new CountDownLatch(1);
        AsyncInitializer initializer = new AsyncInitializer(initializationLatch, handler);
        Thread initThread = new Thread(initializer);
        initThread.start();
        try {
            long curTime = Instant.now().toEpochMilli();
            // account for the time it took to call the various constructors with the actual start time + a grace of 500ms
            long awaitTime = (actualStartTime + LAMBDA_MAX_INIT_TIME_MS) - curTime - INIT_GRACE_TIME_MS;
            log.info("Async initialization will wait for " + awaitTime + "ms");
            if (!initializationLatch.await(awaitTime, TimeUnit.MILLISECONDS)) {
                log.info("Initialization took longer than " + LAMBDA_MAX_INIT_TIME_MS + ", setting new CountDownLatch and " +
                        "continuing in event handler");
                initializationLatch = new CountDownLatch(1);
                initializer.replaceLatch(initializationLatch);
            }
        } catch (InterruptedException e) {
            // at the moment we assume that this happened because of a timeout since the init thread calls System.exit
            // when an exception is thrown.
            throw new ContainerInitializationException("Container initialization interrupted", e);
        }
    }

    public long getActualStartTimeMs() {
        return actualStartTime;
    }

    @Override
    public CountDownLatch getInitializationLatch() {
        return initializationLatch;
    }

    private static class AsyncInitializer implements Runnable {
        private LambdaContainerHandler handler;
        private CountDownLatch initLatch;
        private Logger log = LoggerFactory.getLogger(AsyncInitializationWrapper.class);

        AsyncInitializer(CountDownLatch latch, LambdaContainerHandler h) {
            initLatch = latch;
            handler = h;
        }

        synchronized void replaceLatch(CountDownLatch newLatch) {
            initLatch = newLatch;
        }

        @Override
        @SuppressFBWarnings("DM_EXIT")
        public void run() {
            log.info("Starting async initializer");
            try {
                handler.initialize();
            } catch (ContainerInitializationException e) {
                log.error("Failed to initialize container handler", e);
                // we cannot return the exception so we crash the whole kaboodle here
                System.exit(1);
            }
            synchronized(this) {
                initLatch.countDown();
            }
        }
    }
}
