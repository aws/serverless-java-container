package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.serverless.proxy.spring.springbootapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springbootapp.TestController;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;
import java.util.Map;

import static org.junit.Assert.*;


public class SpringBootAppTest {
    private LambdaHandler handler = new LambdaHandler();
    private MockLambdaContext context = new MockLambdaContext();
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void testMethod_springSecurity_doesNotThrowException() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, TestController.TEST_VALUE);
    }

    @Test
    public void defaultError_requestForward_springBootForwardsToDefaultErrorPage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test2", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(404, resp.getStatusCode());
        assertNotNull(resp.getMultiValueHeaders());
        assertTrue(resp.getMultiValueHeaders().containsKey("Content-Type"));
        assertEquals("application/json;charset=UTF-8", resp.getMultiValueHeaders().getFirst("Content-Type"));
        try {
            JsonNode errorData = mapper.readTree(resp.getBody());
            assertNotNull(errorData.findValue("status"));
            assertEquals(404, errorData.findValue("status").asInt());
            assertNotNull(errorData.findValue("message"));
            assertEquals("No message available", errorData.findValue("message").asText());

        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void requestUri_dotInPathParam_expectRoutingToMethod() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test/testdomain.com", "GET").build();

        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, "testdomain.com");
    }

    @Test
    public void queryString_commaSeparatedList_expectUnmarshalAsList() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test/query-string", "GET")
                                      .queryString("list", "v1,v2,v3").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, "3");
    }

    private void validateSingleValueModel(AwsProxyResponse output, String value) {
        try {
            SingleValueModel response = mapper.readValue(output.getBody(), SingleValueModel.class);
            assertNotNull(response.getValue());
            assertEquals(value, response.getValue());
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
