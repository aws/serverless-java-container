package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.servletapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.servletapp.MessageController;
import com.amazonaws.serverless.proxy.spring.servletapp.MessageData;
import com.amazonaws.serverless.proxy.spring.servletapp.UserData;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

@RunWith(Parameterized.class)
public class ServletAppTest {

    LambdaHandler handler;
    MockLambdaContext lambdaContext = new MockLambdaContext();

    private String type;

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { "API_GW", "ALB", "HTTP_API" });
    }

    public ServletAppTest(String reqType) {
        type = reqType;
        handler = new LambdaHandler(type);
    }

    @Test
    public void helloRequest_respondsWithSingleMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/hello", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        Assert.assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }

    @Test
    public void validateRequest_invalidData_respondsWith400() {
        UserData ud = new UserData();
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/validate", "POST")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                .body(ud);
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
                .body(ud2);
        resp = handler.handleRequest(req, lambdaContext);
        assertEquals("1", resp.getBody());
        assertEquals(400, resp.getStatusCode());
    }

    @Test
    public void messageObject_parsesObject_returnsCorrectMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/message", "POST")
                .json()
                .body(new MessageData("test message"));
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("test message", resp.getBody());
    }

    @Test
    public void messageObject_propertiesInContentType_returnsCorrectMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/message", "POST")
                .header(HttpHeaders.CONTENT_TYPE, "application/json;v=1")
                .header(HttpHeaders.ACCEPT, "application/json;v=1")
                .body(new MessageData("test message"));
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("test message", resp.getBody());
    }

    @Test
    public void echoMessage_fileNameLikeParameter_returnsMessage() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/echo/test.test.test", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("test.test.test", resp.getBody());
    }
}
