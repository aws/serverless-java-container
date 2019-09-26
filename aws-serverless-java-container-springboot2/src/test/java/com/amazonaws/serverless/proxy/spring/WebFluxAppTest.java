package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.webfluxapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.webfluxapp.MessageController;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class WebFluxAppTest {

    LambdaHandler handler = new LambdaHandler();
    MockLambdaContext lambdaContext = new MockLambdaContext();

    @Test
    public void helloRequest_respondsWithSingleMessage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/single", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        Assert.assertEquals(MessageController.MESSAGE, resp.getBody());
    }

    @Test
    public void helloDoubleRequest_respondsWithDoubleMessage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/double", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);

        assertEquals(MessageController.MESSAGE + MessageController.MESSAGE, resp.getBody());
    }
}
