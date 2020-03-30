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
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.SpringVersion;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Base64;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;


public class SpringBootSecurityTest {
    private LambdaHandler handler = new LambdaHandler();
    private MockLambdaContext context = new MockLambdaContext();
    private ObjectMapper mapper = new ObjectMapper();

    @BeforeClass
    public static void before() {
        // We skip the tests if we are running against Spring 5.2.x - SpringBoot 1.5 is deprecated and no longer
        // breaking changes in the latest Spring releases have not been supported in it.
        // TODO: Update the check to verify any Spring version above 5.2
        assumeFalse(Objects.requireNonNull(SpringVersion.getVersion()).startsWith("5.2"));
    }

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
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }
}
