package com.amazonaws.serverless.proxy.spark.embeddedserver;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.jetty.websocket.WebSocketHandlerWrapper;
import spark.http.matching.MatcherFilter;
import spark.route.Routes;
import spark.ssl.SslStores;
import spark.staticfiles.StaticFilesConfiguration;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
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
        applicationRoutes = routes;
        staticFilesConfiguration = filesConfig;
        hasMultipleHandler = multipleHandlers;
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServer
    //-------------------------------------------------------------
    @Override
    public int ignite(String s, int i, SslStores sslStores, int i1, int i2, int i3)
            throws Exception {
        log.info("Starting Spark server, ignoring port and host");
        sparkFilter = new MatcherFilter(applicationRoutes, staticFilesConfiguration, false, hasMultipleHandler);
        sparkFilter.init(null);

        //countDownLatch.countDown();

        return 0;
    }


    public void configureWebSockets(Map<String, WebSocketHandlerWrapper> webSocketHandlers,
                                    Optional<Integer> webSocketIdleTimeoutMillis) {
        // Swallowing this exception to prevent Spark from getting stuck
        // throw new UnsupportedOperationException();
        log.info("Spark called configureWebSockets. However, web sockets are not supported");
    }


    @Override
    public void join()
            throws InterruptedException {
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

    public void handle(HttpServletRequest request, HttpServletResponse response)
            throws IOException, ServletException {
        sparkFilter.doFilter(request, response, null);
    }
}
