package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServer;
import com.amazonaws.serverless.proxy.spark.embeddedserver.LambdaEmbeddedServerFactory;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import spark.Spark;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.reset;
import static org.mockito.Mockito.when;
import static spark.Spark.get;
import static spark.Spark.initExceptionHandler;


public class InitExceptionHandlerTest {

    private static final String TEST_EXCEPTION_MESSAGE = "test exception";
    private static LambdaEmbeddedServer embeddedServer = mock(LambdaEmbeddedServer.class);

    @Test
    void initException_mockException_expectHandlerToRun() {
        try {

            when(embeddedServer.ignite(anyString(), anyInt(), any(), anyInt(), anyInt(), anyInt()))
                                              .thenThrow(new ContainerInitializationException(TEST_EXCEPTION_MESSAGE, null));
            LambdaEmbeddedServerFactory serverFactory = new LambdaEmbeddedServerFactory(embeddedServer);
            new SparkLambdaContainerHandler<>(AwsProxyRequest.class,
                                              AwsProxyResponse.class,
                                              new AwsProxyHttpServletRequestReader(),
                                              new AwsProxyHttpServletResponseWriter(),
                                              new AwsProxySecurityContextWriter(),
                                              new AwsProxyExceptionHandler(),
                                              serverFactory);

            configureRoutes();
            Spark.awaitInitialization();
        } catch (Exception e) {
            e.printStackTrace();
            fail("Error while mocking server");
        }

    }

    @AfterAll
    public static void stopSpark() {
        // un-mock the embedded server to avoid blocking other tests
        reset(embeddedServer);
        // reset the static variable in the factory so that it will be instantiated again next time
        new LambdaEmbeddedServerFactory(null);
        Spark.stop();
    }

    private static void configureRoutes() {
        initExceptionHandler((e) -> {
            assertEquals(TEST_EXCEPTION_MESSAGE, e.getLocalizedMessage());
        });

        get("/test-route", (req, res) -> {
            res.status(200);
            return "test";
        });
    }
}
