package com.amazonaws.serverless.proxy.spark.embeddedserver;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
    private MatcherFilter sparkFilter;
    private StaticFilesConfiguration staticFilesConfiguration;
    private boolean hasMultipleHandler;
    private Logger log = LoggerFactory.getLogger(LambdaEmbeddedServer.class);


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    LambdaEmbeddedServer(Routes routes, StaticFilesConfiguration filesConfig, boolean multipleHandlers) {
        Timer.start("SPARK_EMBEDDED_SERVER_CONSTRUCTOR");
        applicationRoutes = routes;
        staticFilesConfiguration = filesConfig;
        hasMultipleHandler = multipleHandlers;

        // try to initialize the filter here.
        sparkFilter = new MatcherFilter(applicationRoutes, staticFilesConfiguration, true, hasMultipleHandler);
        Timer.stop("SPARK_EMBEDDED_SERVER_CONSTRUCTOR");
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServer
    //-------------------------------------------------------------
    @Override
    public int ignite(String s, int i, SslStores sslStores, int i1, int i2, int i3)
            throws ContainerInitializationException {
        Timer.start("SPARK_EMBEDDED_IGNITE");
        log.info("Starting Spark server, ignoring port and host");
        // if not initialized yet
        if (sparkFilter == null) {
            sparkFilter = new MatcherFilter(applicationRoutes, staticFilesConfiguration, true, hasMultipleHandler);
        }
        sparkFilter.init(null);
        Timer.stop("SPARK_EMBEDDED_IGNITE");
        return i;
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
