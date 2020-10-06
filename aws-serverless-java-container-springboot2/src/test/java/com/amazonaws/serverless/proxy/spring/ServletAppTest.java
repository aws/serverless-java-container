package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.spring.servletapp.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Arrays;
import java.util.Collection;
import java.util.stream.Collectors;

import static org.junit.Assert.*;

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

    @Test
    public void getUtf8String_returnsValidUtf8String() {
        // We expect strings to come back as UTF-8 correctly because Spring itself will call the setCharacterEncoding
        // method on the response to set it to UTF-
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset(ContainerConfig.DEFAULT_CONTENT_CHARSET);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/content-type/utf8", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("text/plain; charset=UTF-8", resp.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).stream().collect(Collectors.joining(",")));
        assertEquals(MessageController.UTF8_RESPONSE, resp.getBody());
    }

    @Test
    public void getUtf8Json_returnsValidUtf8String() {
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset(ContainerConfig.DEFAULT_CONTENT_CHARSET);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/content-type/jsonutf8", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("{\"s\":\""+MessageController.UTF8_RESPONSE+"\"}", resp.getBody());
    }

    @Test
    public void stream_getUtf8String_returnsValidUtf8String() throws IOException {
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset(ContainerConfig.DEFAULT_CONTENT_CHARSET);
        LambdaStreamHandler streamHandler = new LambdaStreamHandler(type);
        AwsProxyRequestBuilder reqBuilder = new AwsProxyRequestBuilder("/content-type/utf8", "GET")
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);
        InputStream req = null;
        switch (type) {
            case "ALB":
                req = reqBuilder.alb().buildStream();
                break;
            case "API_GW":
                req = reqBuilder.buildStream();
                break;
            case "HTTP_API":
                req = reqBuilder.toHttpApiV2RequestStream();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamHandler.handleRequest(req, out, lambdaContext);
        AwsProxyResponse resp = LambdaContainerHandler.getObjectMapper().readValue(out.toByteArray(), AwsProxyResponse.class);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals(MessageController.UTF8_RESPONSE, resp.getBody());
    }

    @Test
    public void stream_getUtf8Json_returnsValidUtf8String() throws IOException {
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset(ContainerConfig.DEFAULT_CONTENT_CHARSET);
        LambdaStreamHandler streamHandler = new LambdaStreamHandler(type);
        AwsProxyRequestBuilder reqBuilder = new AwsProxyRequestBuilder("/content-type/jsonutf8", "GET");
        InputStream req = null;
        switch (type) {
            case "ALB":
                req = reqBuilder.alb().buildStream();
                break;
            case "API_GW":
                req = reqBuilder.buildStream();
                break;
            case "HTTP_API":
                req = reqBuilder.toHttpApiV2RequestStream();
        }
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        streamHandler.handleRequest(req, out, lambdaContext);
        AwsProxyResponse resp = LambdaContainerHandler.getObjectMapper().readValue(out.toByteArray(), AwsProxyResponse.class);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        assertEquals("{\"s\":\""+MessageController.UTF8_RESPONSE+"\"}", resp.getBody());
    }

    @Test
    public void springExceptionMapping_throw404Ex_expectMappedTo404() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/ex/customstatus", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertNotNull(resp);
        assertEquals(404, resp.getStatusCode());
    }

    @Test
    public void echoMessage_populatesSingleValueHeadersForHttpApiV2() {
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/message", "POST")
                .header(HttpHeaders.CONTENT_TYPE, "application/json;v=1")
                .header(HttpHeaders.ACCEPT, "application/json;v=1")
                .body(new MessageData("test message"));
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        if ("HTTP_API".equals(type)) {
            assertNotNull(resp.getHeaders());
        } else {
            assertNull(resp.getHeaders());
        }
    }
}
