package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.serverless.proxy.spring.springbootapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springbootapp.TestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

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
        validateSingleValueModel(resp, TestController.TEST_VALUE);
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
