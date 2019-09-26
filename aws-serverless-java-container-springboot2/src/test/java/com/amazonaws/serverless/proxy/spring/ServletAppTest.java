package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.servletapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.servletapp.MessageController;
import org.junit.Assert;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class ServletAppTest {

    LambdaHandler handler = new LambdaHandler();
    MockLambdaContext lambdaContext = new MockLambdaContext();

    @Test
    public void helloRequest_respondsWithSingleMessage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        Assert.assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }
}
