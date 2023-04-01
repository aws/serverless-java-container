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
package com.amazonaws.serverless.proxy.jersey;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.jersey.model.MapResponseModel;
import com.amazonaws.serverless.proxy.jersey.model.SingleValueModel;
import com.amazonaws.serverless.proxy.jersey.providers.ServletRequestFilter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.junit.jupiter.api.Assertions.fail;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Unit test class for the Jersey AWS_PROXY default implementation
 */
public class JerseyAwsProxyTest {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String USER_PRINCIPAL = "user1";


    private static ObjectMapper objectMapper = new ObjectMapper();

    private static ResourceConfig app = new ResourceConfig().packages("com.amazonaws.serverless.proxy.jersey")
    .register(LoggingFeature.class)
    .register(ServletRequestFilter.class)
    .register(MultiPartFeature.class)
    .register(new ResourceBinder())
    .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER, LoggingFeature.Verbosity.PAYLOAD_ANY);

    private static ResourceConfig httpApiApp = new ResourceConfig().packages("com.amazonaws.serverless.proxy.jersey")
    .register(LoggingFeature.class)
    .register(ServletRequestFilter.class)
    .register(MultiPartFeature.class)
    .register(new ResourceBinder())
    .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER, LoggingFeature.Verbosity.PAYLOAD_ANY);

    private static ResourceConfig appWithoutRegisteredDependencies = new ResourceConfig()
    .packages("com.amazonaws.serverless.proxy.jersey")
    .register(LoggingFeature.class)
    .register(ServletRequestFilter.class)
    .register(MultiPartFeature.class)
    .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER, LoggingFeature.Verbosity.PAYLOAD_ANY);

    private static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static JerseyLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler;

    private static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handlerWithoutRegisteredDependencies
    = JerseyLambdaContainerHandler.getAwsProxyHandler(appWithoutRegisteredDependencies);

    private static Context lambdaContext = new MockLambdaContext();

    private String type;

    public void initJerseyAwsProxyTest(String reqType) {
        type = reqType;
    }

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    private AwsProxyRequestBuilder getRequestBuilder(String path, String method) {

        return new AwsProxyRequestBuilder(path, method);
    }

    private AwsProxyResponse executeRequest(AwsProxyRequestBuilder requestBuilder, Context lambdaContext) {
        switch (type) {
            case "API_GW":
                if (handler == null) {
                    handler = JerseyLambdaContainerHandler.getAwsProxyHandler(app);
                }
                return handler.proxy(requestBuilder.build(), lambdaContext);
            case "ALB":
                if (handler == null) {
                    handler = JerseyLambdaContainerHandler.getAwsProxyHandler(app);
                }
                return handler.proxy(requestBuilder.alb().build(), lambdaContext);
            case "HTTP_API":
                if (httpApiHandler == null) {
                    httpApiHandler = JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(httpApiApp);
                }
                return httpApiHandler.proxy(requestBuilder.toHttpApiV2Request(), lambdaContext);
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }

    private JerseyLambdaContainerHandler getHandler() {
        switch (type) {
            case "API_GW":
            case "ALB":
                return handler;
            case "HTTP_API":
                return httpApiHandler;
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
    }

    @MethodSource("data")
    @ParameterizedTest
    void headers_getHeaders_echo(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/headers", "GET")
        .json()
        .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void headers_servletRequest_echo(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/servlet-headers", "GET")
        .json()
        .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void headers_servletRequest_failedDependencyInjection_expectInternalServerError(String reqType) {
        initJerseyAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequest request = getRequestBuilder("/echo/servlet-headers", "GET")
        .json()
        .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
        .build();

        AwsProxyResponse output = handlerWithoutRegisteredDependencies.proxy(request, lambdaContext);
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));
        assertEquals(Response.Status.INTERNAL_SERVER_ERROR.getStatusCode(), output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void context_servletResponse_setCustomHeader(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/servlet-response", "GET")
        .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey(EchoJerseyResource.SERVLET_RESP_HEADER_KEY));
    }

    @MethodSource("data")
    @ParameterizedTest
    void context_serverInfo_correctContext(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/servlet-context", "GET");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, AwsServletContext.SERVER_INFO);
    }

    @MethodSource("data")
    @ParameterizedTest
    void requestScheme_valid_expectHttps(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/scheme", "GET")
        .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, "https");
    }

    @MethodSource("data")
    @ParameterizedTest
    void requestFilter_injectsServletRequest_expectCustomAttribute(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/filter-attribute", "GET")
        .json();

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, ServletRequestFilter.FILTER_ATTRIBUTE_VALUE);
    }

    @MethodSource("data")
    @ParameterizedTest
    void authorizer_securityContext_customPrincipalSuccess(String reqType) {
        initJerseyAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type)); // TODO: We should figure out a way to run this for HTTP_API too
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/authorizer-principal", "GET")
        .json()
        .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));
        validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
    }

    @MethodSource("data")
    @ParameterizedTest
    void authorizer_securityContext_customAuthorizerContextSuccess(String reqType) {
        initJerseyAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/authorizer-context", "GET")
        .json()
        .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID)
        .authorizerContextValue(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
        .queryString("key", CUSTOM_HEADER_KEY);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @MethodSource("data")
    @ParameterizedTest
    void errors_unknownRoute_expect404(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/test33", "GET");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_contentType_invalidContentType(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/json-body", "POST")
        .header("Content-Type", "application/octet-stream")
        .body("asdasdasd");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_statusCode_methodNotAllowed(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/status-code", "POST")
        .json()
        .queryString("status", "201");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void responseBody_responseWriter_validBody(String reqType) throws JsonProcessingException {
        initJerseyAwsProxyTest(reqType);
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/json-body", "POST")
        .json()
        .body(objectMapper.writeValueAsString(singleValueModel));

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());

        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @MethodSource("data")
    @ParameterizedTest
    void statusCode_responseStatusCode_customStatusCode(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/status-code", "GET")
        .json()
        .queryString("status", "201");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void base64_binaryResponse_base64Encoding(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/binary", "GET");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @MethodSource("data")
    @ParameterizedTest
    void exception_mapException_mapToNotImplemented(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/exception", "GET");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals(EchoJerseyResource.EXCEPTION_MESSAGE, response.getBody());
        assertEquals(Response.Status.NOT_IMPLEMENTED.getStatusCode(), response.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void stripBasePath_route_shouldRouteCorrectly(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/custompath/echo/status-code", "GET")
        .json()
        .queryString("status", "201");
        getHandler().stripBasePath("/custompath");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
        getHandler().stripBasePath("");
    }

    @MethodSource("data")
    @ParameterizedTest
    void stripBasePath_route_shouldReturn404WithStageAsContext(String reqType) {
        initJerseyAwsProxyTest(reqType);
        assumeTrue(!"ALB".equals(type));
        AwsProxyRequestBuilder request = getRequestBuilder("/custompath/echo/status-code", "GET")
        .stage("prod")
        .json()
        .queryString("status", "201");
        getHandler().stripBasePath("/custompath");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        getHandler().stripBasePath("");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @MethodSource("data")
    @ParameterizedTest
    void stripBasePath_route_shouldReturn404(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/custompath/echo/status-code", "GET")
        .json()
        .queryString("status", "201");
        getHandler().stripBasePath("/custom");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
        getHandler().stripBasePath("");
    }

    @MethodSource("data")
    @ParameterizedTest
    void securityContext_injectPrincipal_expectPrincipalName(String reqType) {
        initJerseyAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/security-context", "GET")
        .authorizerPrincipal(USER_PRINCIPAL);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, USER_PRINCIPAL);
    }

    @MethodSource("data")
    @ParameterizedTest
    void emptyStream_putNullBody_expectPutToSucceed(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/empty-stream/" + CUSTOM_HEADER_KEY + "/test/2", "PUT")
        .nullBody()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, CUSTOM_HEADER_KEY);
    }

    @MethodSource("data")
    @ParameterizedTest
    void refererHeader_headerParam_expectCorrectInjection(String reqType) {
        initJerseyAwsProxyTest(reqType);
        String refererValue = "test-referer";
        AwsProxyRequestBuilder request = getRequestBuilder("/echo/referer-header", "GET")
        .nullBody()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .header("Referer", refererValue);

        AwsProxyResponse resp = executeRequest(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, refererValue);
    }

    @MethodSource("data")
    @ParameterizedTest
    void textPlainContent_plain_responseHonorsContentType(String reqType) {
        initJerseyAwsProxyTest(reqType);
        AwsProxyRequestBuilder req = getRequestBuilder("/echo/plain", "GET")
        .nullBody()
        .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
        .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN);

        AwsProxyResponse resp = executeRequest(req, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.TEXT_PLAIN, resp.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).get(0));
    }

    private void validateMapResponseModel(AwsProxyResponse output) {
        validateMapResponseModel(output, CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
    }

    private void validateMapResponseModel(AwsProxyResponse output, String key, String value) {
        try {
            MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
            assertNotNull(response.getValues().get(key));
            assertEquals(value, response.getValues().get(key));
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }

    private void validateSingleValueModel(AwsProxyResponse output, String value) {
        try {
            SingleValueModel response = objectMapper.readValue(output.getBody(), SingleValueModel.class);
            assertNotNull(response.getValue());
            assertEquals(value, response.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Exception while parsing response body: " + e.getMessage());
        }
    }
}
