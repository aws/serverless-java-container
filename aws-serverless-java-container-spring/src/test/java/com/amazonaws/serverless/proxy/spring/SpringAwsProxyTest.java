package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletRegistration;
import com.amazonaws.serverless.proxy.model.*;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoResource;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.RestControllerAdvice;
import com.amazonaws.serverless.proxy.spring.echoapp.UnauthenticatedFilter;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.web.servlet.DispatcherServlet;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.Arrays;
import java.util.Collection;
import java.util.EnumSet;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assumptions.assumeFalse;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

public class SpringAwsProxyTest {
    private static final String CUSTOM_HEADER_KEY = "x-custom-header";
    private static final String CUSTOM_HEADER_VALUE = "my-custom-value";
    private static final String AUTHORIZER_PRINCIPAL_ID = "test-principal-" + UUID.randomUUID().toString();
    private static final String UNICODE_VALUE = "שלום לכולם";

    private ObjectMapper objectMapper = new ObjectMapper();
    private MockLambdaContext lambdaContext = new MockLambdaContext();
    private static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static SpringLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler;

    private AwsLambdaServletContainerHandler.StartupHandler h = (c -> {
        FilterRegistration.Dynamic registration = c.addFilter("UnauthenticatedFilter", UnauthenticatedFilter.class);
        // update the registration to map to a path
        registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/echo/*");
        // servlet name mappings are disabled and will throw an exception

        //handler.getApplicationInitializer().getDispatcherServlet().setThrowExceptionIfNoHandlerFound(true);
        ((DispatcherServlet)((AwsServletRegistration)c.getServletRegistration("dispatcherServlet")).getServlet()).setThrowExceptionIfNoHandlerFound(true);
    });

    private String type;

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    public void initSpringAwsProxyTest(String reqType) {
        type = reqType;
    }

    private AwsProxyResponse executeRequest(AwsProxyRequestBuilder requestBuilder, Context lambdaContext) {
        try {
            switch (type) {
                case "API_GW":
                    if (handler == null) {
                        handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
                        handler.onStartup(h);
                    }
                    return handler.proxy(requestBuilder.build(), lambdaContext);
                case "ALB":
                    if (handler == null) {
                        handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
                        handler.onStartup(h);
                    }
                    return handler.proxy(requestBuilder.alb().build(), lambdaContext);
                case "HTTP_API":
                    if (httpApiHandler == null) {
                        httpApiHandler = SpringLambdaContainerHandler.getHttpApiV2ProxyHandler(EchoSpringAppConfig.class);
                        httpApiHandler.onStartup(h);
                    }
                    return httpApiHandler.proxy(requestBuilder.toHttpApiV2Request(), lambdaContext);
                default:
                    throw new RuntimeException("Unknown request type: " + type);
            }
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail("Could not execute request");
            throw new RuntimeException(e);
        }
    }

    @BeforeEach
    public void clearServletContextCache() {
        AwsServletContext.clearServletContextCache();
    }

    @MethodSource("data")
    @ParameterizedTest
    void controllerAdvice_invalidPath_returnAdvice(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo2", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertNotNull(output);
        assertEquals(404, output.getStatusCode());
        validateSingleValueModel(output, RestControllerAdvice.ERROR_MESSAGE);

    }

    @MethodSource("data")
    @ParameterizedTest
    void headers_getHeaders_echo(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void headers_servletRequest_echo(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/servlet-headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryString_uriInfo_echo(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/query-string", "GET")
                .json()
                .queryString(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryString_listParameter_expectCorrectLength(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/list-query-string", "GET")
                .json()
                .queryString("list", "v1,v2,v3");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "3");
    }

    @MethodSource("data")
    @ParameterizedTest
    void queryString_multiParam_expectCorrectValueCount(String reqType)
            throws IOException {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/multivalue-query-string", "GET")
                .json()
                .queryString("multiple", "first")
                .queryString("multiple", "second");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);

        assertEquals(2, response.getValues().size());
        assertTrue(response.getValues().containsKey("first"));
        assertTrue(response.getValues().containsKey("second"));
    }

    @MethodSource("data")
    @ParameterizedTest
    void dateHeader_notModified_expect304(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/last-modified", "GET")
                .json()
                .header(
                        HttpHeaders.IF_MODIFIED_SINCE,
                        DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().minus(1, ChronoUnit.SECONDS))
                );

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(304, output.getStatusCode());
        assertEquals("", output.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void dateHeader_notModified_expect200(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/last-modified", "GET")
                .json()
                .header(
                        HttpHeaders.IF_MODIFIED_SINCE,
                        DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().minus(5, ChronoUnit.DAYS))
                );

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(EchoResource.STRING_BODY, output.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void authorizer_securityContext_customPrincipalSuccess(String reqType) {
        initSpringAwsProxyTest(reqType);
        assumeTrue("API_GW".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/authorizer-principal", "GET")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

        validateSingleValueModel(output, AUTHORIZER_PRINCIPAL_ID);
    }

    @MethodSource("data")
    @ParameterizedTest
    void errors_unknownRoute_expect404(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/test33", "GET");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(404, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_contentType_invalidContentType(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                .header("Content-Type", "application/octet-stream")
                .body("asdasdasd");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(415, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_statusCode_methodNotAllowed(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/status-code", "POST")
                .json()
                .queryString("status", "201");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(405, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void error_unauthenticatedCall_filterStepsRequest(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/status-code", "GET")
                .header(UnauthenticatedFilter.HEADER_NAME, "1")
                .json()
                .queryString("status", "201");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(401, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void responseBody_responseWriter_validBody(String reqType) throws JsonProcessingException {
        initSpringAwsProxyTest(reqType);
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                .json()
                .header("Content-Type", "application/json")
                .body(objectMapper.writeValueAsString(singleValueModel));

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());
        validateSingleValueModel(output, CUSTOM_HEADER_VALUE);
    }

    @MethodSource("data")
    @ParameterizedTest
    void responseBody_responseWriter_validBody_UTF(String reqType) throws JsonProcessingException {
        initSpringAwsProxyTest(reqType);
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(UNICODE_VALUE);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                .header("Content-Type", "application/json; charset=UTF-8")
                .body(objectMapper.writeValueAsString(singleValueModel));
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset("UTF-8");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertNotNull(output.getBody());
        validateSingleValueModel(output, UNICODE_VALUE);
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset(ContainerConfig.DEFAULT_CONTENT_CHARSET);
    }

    @MethodSource("data")
    @ParameterizedTest
    void statusCode_responseStatusCode_customStatusCode(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/status-code", "GET")
                .json()
                .queryString("status", "201");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(201, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void base64_binaryResponse_base64Encoding(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/binary", "GET");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertTrue(Base64.isBase64(response.getBody()));
    }

    @MethodSource("data")
    @ParameterizedTest
    void injectBody_populatedResponse_noException(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/request-body", "POST")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .body("This is a populated body");

        AwsProxyResponse response = executeRequest(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCode());
        try {
            SingleValueModel output = objectMapper.readValue(response.getBody(), SingleValueModel.class);
            assertEquals("true", output.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        AwsProxyRequestBuilder emptyReq = new AwsProxyRequestBuilder("/echo/request-body", "POST");
        AwsProxyResponse emptyResp = executeRequest(emptyReq, lambdaContext);
        try {
            SingleValueModel output = objectMapper.readValue(emptyResp.getBody(), SingleValueModel.class);
            assertNull(output.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @MethodSource("data")
    @ParameterizedTest
    void servletRequestEncoding_acceptEncoding_okStatusCode(String reqType) {
        initSpringAwsProxyTest(reqType);
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequestBuilder request = null;
        try {
            request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .queryString("status", "200")
                    .body(objectMapper.writeValueAsString(singleValueModel));
        } catch (JsonProcessingException e) {
            fail("Could not serialize object to JSON");
        }

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
    }

    @MethodSource("data")
    @ParameterizedTest
    void request_requestURI(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/request-URI", "GET");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "/echo/request-URI");
    }

    @MethodSource("data")
    @ParameterizedTest
    void request_requestURL(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/request-url", "GET")
                .scheme("https")
                .serverName("api.myserver.com")
                .stage("prod");
        handler.stripBasePath("");
        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "https://api.myserver.com/echo/request-url");
    }

    @MethodSource("data")
    @ParameterizedTest
    void request_encodedPath_returnsDecodedPath(String reqType) {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/encoded-request-uri/Some%20Thing", "GET")
                .scheme("https")
                .serverName("api.myserver.com")
                .stage("prod");

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "Some Thing");

    }

    @MethodSource("data")
    @ParameterizedTest
    void contextPath_generateLink_returnsCorrectPath(String reqType) {
        initSpringAwsProxyTest(reqType);
        assumeFalse("ALB".equals(type));
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/generate-uri", "GET")
                .scheme("https")
                .serverName("api.myserver.com")
                .stage("prod");
        LambdaContainerHandler.getContainerConfig().addCustomDomain("api.myserver.com");
        SpringLambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        String expectedUri = "https://api.myserver.com/prod/echo/encoded-request-uri/" + EchoResource.TEST_GENERATE_URI;

        validateSingleValueModel(output, expectedUri);

        SpringLambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @MethodSource("data")
    @ParameterizedTest
    void multipart_getFileName_returnsCorrectFileName(String reqType)
            throws IOException {
        initSpringAwsProxyTest(reqType);
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder("/echo/attachment", "POST")
                .formFilePart("testFile", "myFile.txt", "hello".getBytes());

        AwsProxyResponse output = executeRequest(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        assertEquals("testFile", output.getBody());
    }

    private void validateMapResponseModel(AwsProxyResponse output) {
        try {
            MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
            assertNotNull(response.getValues().get(CUSTOM_HEADER_KEY));
            assertEquals(CUSTOM_HEADER_VALUE, response.getValues().get(CUSTOM_HEADER_KEY));
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

