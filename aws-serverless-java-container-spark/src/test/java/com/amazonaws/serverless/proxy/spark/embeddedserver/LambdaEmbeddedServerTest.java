package com.amazonaws.serverless.proxy.spark.embeddedserver;


import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.fail;


public class LambdaEmbeddedServerTest {
    private static LambdaEmbeddedServer server = new LambdaEmbeddedServer(null, null, null, false);

    @Test
    void webSocket_configureWebSocket_noException() {
        try {
            server.configureWebSockets(null, Optional.of(0L));
        } catch (Exception e) {
            e.printStackTrace();
            fail();
        }
    }
}
