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
package com.amazonaws.serverless.proxy.spark.embeddedserver;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.ExceptionMapper;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.jetty.websocket.WebSocketHandlerWrapper;
import spark.http.matching.MatcherFilter;
import spark.route.Routes;
import spark.ssl.SslStores;
import spark.staticfiles.StaticFilesConfiguration;

import javax.servlet.Filter;
import java.util.Map;
import java.util.Optional;

public class LambdaEmbeddedServer
        implements EmbeddedServer {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Routes applicationRoutes;
    private ExceptionMapper exceptionMapper;
    private MatcherFilter sparkFilter;
    private StaticFilesConfiguration staticFilesConfiguration;
    private boolean hasMultipleHandler;
    private Logger log = LoggerFactory.getLogger(LambdaEmbeddedServer.class);


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    LambdaEmbeddedServer(Routes routes, StaticFilesConfiguration filesConfig, ExceptionMapper exceptionMapper, boolean multipleHandlers) {
        Timer.start("SPARK_EMBEDDED_SERVER_CONSTRUCTOR");
        applicationRoutes = routes;
        staticFilesConfiguration = filesConfig;
        hasMultipleHandler = multipleHandlers;
        this.exceptionMapper = exceptionMapper;

        // try to initialize the filter here.
        sparkFilter = new MatcherFilter(applicationRoutes, staticFilesConfiguration, exceptionMapper, true, hasMultipleHandler);
        Timer.stop("SPARK_EMBEDDED_SERVER_CONSTRUCTOR");
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServer
    //-------------------------------------------------------------
    @Override
    public int ignite(String host, int port, SslStores sslStores, int maxThreads, int minThreads, int threadIdleTimeoutMillis)
            throws ContainerInitializationException {
        Timer.start("SPARK_EMBEDDED_IGNITE");
        log.info("Starting Spark server, ignoring port and host");
        // if not initialized yet
        if (sparkFilter == null) {
            sparkFilter = new MatcherFilter(applicationRoutes, staticFilesConfiguration, exceptionMapper, true, hasMultipleHandler);
        }
        sparkFilter.init(null);
        Timer.stop("SPARK_EMBEDDED_IGNITE");
        return port;
    }


    public void configureWebSockets(Map<String, WebSocketHandlerWrapper> webSocketHandlers,
                                    Optional<Integer> webSocketIdleTimeoutMillis) {
        // Swallowing this exception to prevent Spark from getting stuck
        // throw new UnsupportedOperationException();
        log.info("Spark called configureWebSockets. However, web sockets are not supported");
    }


    @Override
    public void join() {
        log.info("Called join method, nothing to do here since Lambda only runs a single event per container");
    }


    @Override
    public void extinguish() {
        log.info("Called extinguish method, nothing to do here.");
    }


    @Override
    public int activeThreadCount() {
        log.debug("Called activeThreadCount, since Lambda only runs one event per container we always return 1");
        return 1;
    }

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Returns the initialized instance of the main Spark filter.
     * @return The spark filter instance.
     */
    public Filter getSparkFilter() {
        return sparkFilter;
    }
}
