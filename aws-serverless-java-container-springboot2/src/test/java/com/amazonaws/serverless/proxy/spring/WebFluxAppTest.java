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
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class WebFluxAppTest {

    LambdaHandler handler;
    MockLambdaContext lambdaContext = new MockLambdaContext();

    private String type;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { "API_GW", "ALB", "HTTP_API" });
    }

    public WebFluxAppTest(String reqType) {
        type = reqType;
        handler = new LambdaHandler(type);
    }

    @Test
    public void helloRequest_respondsWithSingleMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/single", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        System.out.println(resp.getBody());
        Assert.assertEquals(MessageController.MESSAGE, resp.getBody());
    }

    @Test
    public void helloDoubleRequest_respondsWithDoubleMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/double", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);

        assertEquals(MessageController.MESSAGE + MessageController.MESSAGE, resp.getBody());
    }

    @Test
    public void messageObject_parsesObject_returnsCorrectMessage() throws JsonProcessingException {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/message", "POST")
                .json()
                .body(new MessageData("test message"));
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("test message", resp.getBody());
    }
}
