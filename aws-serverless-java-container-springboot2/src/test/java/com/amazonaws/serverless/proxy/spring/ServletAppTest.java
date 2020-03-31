package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.securityapp.SecurityConfig;
import com.amazonaws.serverless.proxy.spring.servletapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.servletapp.MessageController;
import com.amazonaws.serverless.proxy.spring.servletapp.UserData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

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

    @Test
    public void validateRequest_invalidData_respondsWith400() {
        UserData ud = new UserData();
        AwsProxyRequest req = new AwsProxyRequestBuilder("/validate", "POST")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(ud)
                .build();
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        try {
            System.out.println(LambdaContainerHandler.getObjectMapper().writeValueAsString(resp));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        assertEquals("3", resp.getBody());
        assertEquals(400, resp.getStatusCode());

        UserData ud2 = new UserData();
        ud2.setFirstName("Test");
        ud2.setLastName("Test");
        ud2.setEmail("Test");
        req = new AwsProxyRequestBuilder("/validate", "POST")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(ud2)
                .build();
        System.out.println(req.getBody());
        resp = handler.handleRequest(req, lambdaContext);
        assertEquals("1", resp.getBody());
        assertEquals(400, resp.getStatusCode());
    }
}
