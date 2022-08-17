/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.struts2;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.struts2.echoapp.EchoAction;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.struts2.StrutsJUnit4TestCase;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.*;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.junit.Assume.assumeTrue;

/**
 * Unit test class for the Struts2 AWS_PROXY default implementation
 */
@RunWith(Parameterized.class)
public class Struts2AwsProxyTest extends StrutsJUnit4TestCase<EchoAction> {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String QUERY_STRING_KEY = "message";
    private static final String QUERY_STRING_ENCODED_VALUE = "Hello Struts2";
    private static final String USER_PRINCIPAL = "user1";
    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";


    private static ObjectMapper objectMapper = new ObjectMapper();
    private final Struts2LambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = Struts2LambdaContainerHandler
            .getAwsProxyHandler();
    private final Struts2LambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler = Struts2LambdaContainerHandler
            .getHttpApiV2ProxyHandler();
    private static Context lambdaContext = new MockLambdaContext();

    private String type;

    public Struts2AwsProxyTest(String reqType) {
        type = reqType;
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { "API_GW", "ALB", "HTTP_API" });
    }

    private AwsProxyResponse executeRequest(AwsProxyRequestBuilder requestBuilder, Context lambdaContext) {
        switch (type) {
            case "API_GW":
                return handler.proxy(requestBuilder.build(), lambdaContext);
            case "ALB":
                return handler.proxy(requestBuilder.alb().build(), lambdaContext);
            case "HTTP_API":
                return httpApiHandler.proxy(requestBuilder.toHttpApiV2Request(), lambdaContext);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }
    
    @Test
    public void headers_getHeaders_echo() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "headers")
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @Test
    public void context_servletResponse_setCustomHeader() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "GET")
                .queryString("customHeader", "true")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey("XX"));
    }

    @Test
    public void context_serverInfo_correctContext() {
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "GET")
                .queryString(QUERY_STRING_KEY, "Hello Struts2")
                .header("Content-Type", "application/json")
                .queryString("contentType", "true");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "Hello Struts2");
    }

    @Test
    public void queryString_uriInfo_echo() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "query-string")
                .queryString(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .json();


        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @Test
    public void requestScheme_valid_expectHttps() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "scheme")
                .queryString(QUERY_STRING_KEY, QUERY_STRING_ENCODED_VALUE)
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "https");
    }

    @Test
    public void authorizer_securityContext_customPrincipalSuccess() {
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "principal")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
    }

    @Test
    public void errors_unknownRoute_expect404() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/unknown", "GET");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @Test
    public void error_contentType_invalidContentType() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString("mode", "content-type")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @Test
    public void error_statusCode_methodNotAllowed() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString("mode", "not-allowed")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }


    @Test
    public void responseBody_responseWriter_validBody() throws JsonProcessingException {
        Map<String, String> value = new HashMap<>();
        value.put(QUERY_STRING_KEY, CUSTOM_HEADER_VALUE);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "POST")
                .json()
                .body(objectMapper.writeValueAsString(value));

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());

        validateSingleValueModel(output, "{\"message\":\"my-custom-value\"}");
    }

    @Test
    public void statusCode_responseStatusCode_customStatusCode() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "custom-status-code")
                .queryString("status", "201")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @Test
    public void base64_binaryResponse_base64Encoding() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "GET");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @Test
    public void exception_mapException_mapToNotImplemented() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString("mode", "not-implemented");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals("null", response.getBody());
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
    }

    @Test
    public void stripBasePath_route_shouldRouteCorrectly() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/custompath/echo", "GET")
                .json()
                .queryString(QUERY_STRING_KEY, "stripped");
        handler.stripBasePath("/custompath");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        validateSingleValueModel(output, "stripped");
        handler.stripBasePath("");
    }

    @Test
    public void stripBasePath_route_shouldReturn404() {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/custompath/echo/status-code", "GET")
                .json()
                .queryString("status", "201");
        handler.stripBasePath("/custom");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        handler.stripBasePath("");
    }

    @Test
    public void securityContext_injectPrincipal_expectPrincipalName() {
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "GET")
                .queryString("mode", "principal")
                .authorizerPrincipal(USER_PRINCIPAL);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, USER_PRINCIPAL);
    }

    @Test
    public void queryParam_encoding_expectUnencodedParam() {
        assumeTrue("API_GW".equals(type));
        String paramValue = "p%2Fz%2B3";
        String decodedParam = "";
        try {
            decodedParam = URLDecoder.decode(paramValue, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Could not decode parameter");
        }
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "GET").queryString(QUERY_STRING_KEY, decodedParam);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, decodedParam);
    }

    @Test
    public void queryParam_encoding_expectEncodedParam() {
        assumeTrue("API_GW".equals(type));
        String paramValue = "p%2Fz%2B3";
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "GET").queryString(QUERY_STRING_KEY, paramValue);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, paramValue);
    }


    private void validateMapResponseModel(AwsProxyResponse output) {
        validateMapResponseModel(output, CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
    }

    private void validateMapResponseModel(AwsProxyResponse output, String key, String value) {
        try {
            TypeReference<HashMap<String, Object>> typeRef
                    = new TypeReference<HashMap<String, Object>>() {
            };
            HashMap<String, Object> response = objectMapper.readValue(output.getBody(), typeRef);
            assertNotNull(response.get(key));
            assertEquals(value, response.get(key));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }

    private void validateSingleValueModel(AwsProxyResponse output, String value) {
        try {
            assertNotNull(output.getBody());
            assertEquals(value, objectMapper.readerFor(String.class).readValue(output.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }
}
