package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.serverless.proxy.spring.sbsecurityapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.sbsecurityapp.TestController;
import com.amazonaws.serverless.proxy.spring.sbsecurityapp.TestSecurityConfig;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Base64;

import static org.junit.Assert.*;


public class SpringBootSecurityTest {
    private LambdaHandler handler = new LambdaHandler();
    private MockLambdaContext context = new MockLambdaContext();
    private ObjectMapper mapper = new ObjectMapper();

    @Test
    public void correctUser_springSecurityBasicAuth_requestSucceeds() throws JsonProcessingException {
        String authValue = Base64.getMimeEncoder().encodeToString((TestSecurityConfig.USERNAME + ":" + TestSecurityConfig.PASSWORD).getBytes());
        AwsProxyRequest req = new AwsProxyRequestBuilder("/user", "GET")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue).build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, TestSecurityConfig.USERNAME);
    }

    @Test
    public void wrongUser_springSecurityBasicAuth_requestRedirectsSuccessfully() throws JsonProcessingException {
        String authValue = Base64.getMimeEncoder().encodeToString((TestSecurityConfig.NO_ADMIN_USERNAME + ":" + TestSecurityConfig.PASSWORD).getBytes());
        AwsProxyRequest req = new AwsProxyRequestBuilder("/user", "GET")
                .header(HttpHeaders.AUTHORIZATION, "Basic " + authValue).build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        mapper.enable(SerializationFeature.INDENT_OUTPUT);
        assertEquals(403, resp.getStatusCode());
        validateSingleValueModel(resp, TestController.ACCESS_DENIED);
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
