package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.webfluxapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.webfluxapp.MessageController;
import com.amazonaws.serverless.proxy.spring.webfluxapp.MessageData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

public class WebFluxAppTest {

    LambdaHandler handler;
    MockLambdaContext lambdaContext = new MockLambdaContext();

    private String type;

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    public void initWebFluxAppTest(String reqType) {
        type = reqType;
        handler = new LambdaHandler(type);
    }

    @MethodSource("data")
    @ParameterizedTest
    void helloRequest_respondsWithSingleMessage(String reqType) {
        initWebFluxAppTest(reqType);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/single", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        System.out.println(resp.getBody());
        assertEquals(MessageController.MESSAGE, resp.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void helloDoubleRequest_respondsWithDoubleMessage(String reqType) {
        initWebFluxAppTest(reqType);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/double", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);

        assertEquals(MessageController.MESSAGE + MessageController.MESSAGE, resp.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void messageObject_parsesObject_returnsCorrectMessage(String reqType) throws JsonProcessingException {
        initWebFluxAppTest(reqType);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/message", "POST")
                .json()
                .body(new MessageData("test message"));
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("test message", resp.getBody());
    }
}
