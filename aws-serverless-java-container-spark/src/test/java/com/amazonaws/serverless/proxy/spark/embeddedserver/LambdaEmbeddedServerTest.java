package com.amazonaws.serverless.proxy.spark.embeddedserver;


import org.junit.Test;

import java.util.Optional;

import static org.junit.Assert.*;


public class LambdaEmbeddedServerTest {
    private static LambdaEmbeddedServer server = new LambdaEmbeddedServer(null, null, null, false);

    @Test
    public void webSocket_configureWebSocket_noException() {
        try {
            server.configureWebSockets(null, Optional.of(0));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
