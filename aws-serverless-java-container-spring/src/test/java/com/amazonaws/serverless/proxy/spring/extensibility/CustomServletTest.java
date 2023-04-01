package com.amazonaws.serverless.proxy.spring.extensibility;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.HttpMethod;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomServletTest {

    @Test
    void customServlet() throws IOException {
        StreamLambdaHandler lambdaHandler = new StreamLambdaHandler();
        InputStream requestStream = new AwsProxyRequestBuilder("/test", HttpMethod.GET)
                .buildStream();
        ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
        lambdaHandler.handleRequest(requestStream, responseStream, new MockLambdaContext());
        assertTrue(responseStream.toString().contains("Unittest"),
                "response should contain value set in CustomServlet");
    }

}
