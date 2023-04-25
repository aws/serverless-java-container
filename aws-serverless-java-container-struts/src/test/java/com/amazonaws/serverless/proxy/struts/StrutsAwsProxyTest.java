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
package com.amazonaws.serverless.proxy.struts;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.struts.echoapp.EchoAction;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.apache.struts2.junit.StrutsRestTestCase;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit test class for the Struts2 AWS_PROXY default implementation
 */
public class StrutsAwsProxyTest extends StrutsRestTestCase<EchoAction> {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String HTTP_METHOD_GET = "GET";
    private static final String QUERY_STRING_MODE = "mode";
    private static final String QUERY_STRING_KEY = "message";
    private static final String QUERY_STRING_ENCODED_VALUE = "Hello Struts2";
    private static final String USER_PRINCIPAL = "user1";
    private static final String CONTENT_TYPE_APPLICATION_JSON = "application/json; charset=UTF-8";


    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final StrutsLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = StrutsLambdaContainerHandler
            .getAwsProxyHandler();
    private final StrutsLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler = StrutsLambdaContainerHandler
            .getHttpApiV2ProxyHandler();
    private final Context lambdaContext = new MockLambdaContext();
    private String type;

    public void initStrutsAwsProxyTest(String reqType) {
        type = reqType;
    }

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
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

    @MethodSource("data")
    @ParameterizedTest
    void headers_getHeaders_echo(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "headers")
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void context_servletResponse_setCustomHeader(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", HTTP_METHOD_GET)
                .queryString("customHeader", "true")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey("XX"));
    }

    @MethodSource("data")
    @ParameterizedTest
    void context_serverInfo_correctContext(String reqType) {
        initStrutsAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_KEY, "Hello Struts2")
                .header("Content-Type", "application/json")
                .queryString("contentType", "true");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "Hello Struts2");
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryString_uriInfo_echo(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "query-string")
                .queryString(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .json();


        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void requestScheme_valid_expectHttps(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "scheme")
                .queryString(QUERY_STRING_KEY, QUERY_STRING_ENCODED_VALUE)
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "https");
    }

    @MethodSource("data")
    @ParameterizedTest
    void authorizer_securityContext_customPrincipalSuccess(String reqType) {
        initStrutsAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "principal")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(CONTENT_TYPE_APPLICATION_JSON, output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
    }

    @MethodSource("data")
    @ParameterizedTest
    void errors_unknownRoute_expect404(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/unknown", HTTP_METHOD_GET);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_contentType_invalidContentType(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString(QUERY_STRING_MODE, "content-type")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_statusCode_methodNotAllowed(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString(QUERY_STRING_MODE, "not-allowed")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }


    @MethodSource("data")
    @ParameterizedTest
    void responseBody_responseWriter_validBody(String reqType) throws JsonProcessingException {
        initStrutsAwsProxyTest(reqType);
        Map<String, String> value = new HashMap<>();
        value.put(QUERY_STRING_KEY, CUSTOM_HEADER_VALUE);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", "POST")
                .json()
                .body(OBJECT_MAPPER.writeValueAsString(value));

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());

        validateSingleValueModel(output, "{\"message\":\"my-custom-value\"}");
    }

    @MethodSource("data")
    @ParameterizedTest
    void statusCode_responseStatusCode_customStatusCode(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "custom-status-code")
                .queryString("status", "201")
                .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void base64_binaryResponse_base64Encoding(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", HTTP_METHOD_GET);

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @MethodSource("data")
    @ParameterizedTest
    void exception_mapException_mapToNotImplemented(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", "POST")
                .queryString(QUERY_STRING_MODE, "not-implemented");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals("null", response.getBody());
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void stripBasePath_route_shouldRouteCorrectly(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/custompath/echo", HTTP_METHOD_GET)
                .json()
                .queryString(QUERY_STRING_KEY, "stripped");
        handler.stripBasePath("/custompath");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        validateSingleValueModel(output, "stripped");
        handler.stripBasePath("");
    }

    @MethodSource("data")
    @ParameterizedTest
    void stripBasePath_route_shouldReturn404(String reqType) {
        initStrutsAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/custompath/echo/status-code", HTTP_METHOD_GET)
                .json()
                .queryString("status", "201");
        handler.stripBasePath("/custom");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        handler.stripBasePath("");
    }

    @MethodSource("data")
    @ParameterizedTest
    void securityContext_injectPrincipal_expectPrincipalName(String reqType) {
        initStrutsAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo-request-info", HTTP_METHOD_GET)
                .queryString(QUERY_STRING_MODE, "principal")
                .authorizerPrincipal(USER_PRINCIPAL);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, USER_PRINCIPAL);
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryParam_encoding_expectUnencodedParam(String reqType) {
        initStrutsAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        String paramValue = "p%2Fz%2B3";
        String decodedParam = "";
        try {
            decodedParam = URLDecoder.decode(paramValue, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Could not decode parameter");
        }
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", HTTP_METHOD_GET).queryString(QUERY_STRING_KEY, decodedParam);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, decodedParam);
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryParam_encoding_expectEncodedParam(String reqType) {
        initStrutsAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        String paramValue = "p%2Fz%2B3";
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo", HTTP_METHOD_GET).queryString(QUERY_STRING_KEY, paramValue);

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
            HashMap<String, Object> response = OBJECT_MAPPER.readValue(output.getBody(), typeRef);
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
            assertEquals(value, OBJECT_MAPPER.readerFor(String.class).readValue(output.getBody()));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }
}
