package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import java.io.IOException;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EchoSpringAppConfig.class})
@WebAppConfiguration
@TestExecutionListeners(inheritListeners = false, listeners = {DependencyInjectionTestExecutionListener.class})
public class SpringAwsProxyTest {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockLambdaContext lambdaContext;

    @Autowired
    private SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @Test
    public void headers_getHeaders_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getHeaders().get("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @Test
    public void headers_servletRequest_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/servlet-headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getHeaders().get("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @Test
    public void queryString_uriInfo_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/query-string", "GET")
                .json()
                .queryString(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getHeaders().get("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @Test
    public void authorizer_securityContext_customPrincipalSuccess() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/authorizer-principal", "GET")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getHeaders().get("Content-Type").split(";")[0]);

        validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
    }

    @Test
    public void errors_unknownRoute_expect404() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/test33", "GET").build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @Test
    public void error_contentType_invalidContentType() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @Test
    public void error_statusCode_methodNotAllowed() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/status-code", "POST")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }

    @Test
    public void responseBody_responseWriter_validBody() throws JsonProcessingException {
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                .json()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(singleValueModel))
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());
        System.out.println("Output:" + output.getBody());
        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @Test
    public void statusCode_responseStatusCode_customStatusCode() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/status-code", "GET")
                .json()
                .queryString("status", "201")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @Test
    public void base64_binaryResponse_base64Encoding() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/binary", "GET").build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    private void validateMapResponseModel(AwsProxyResponse output) {
        try {
            MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
            assertNotNull(response.getValues().get(CUSTOM_HEADER_KEY));
            assertEquals(CUSTOM_HEADER_VALUE, response.getValues().get(CUSTOM_HEADER_KEY));
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void validateSingleValueModel(AwsProxyResponse output, String value) {
        try {
            SingleValueModel response = objectMapper.readValue(output.getBody(), SingleValueModel.class);
            assertNotNull(response.getValue());
            assertEquals(value, response.getValue());
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }
}

