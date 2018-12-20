package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoResource;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.RestControllerAdvice;
import com.amazonaws.serverless.proxy.spring.echoapp.UnauthenticatedFilter;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.commons.codec.binary.Base64;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.UUID;

import static org.junit.Assert.*;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EchoSpringAppConfig.class})
@WebAppConfiguration
@TestExecutionListeners(inheritListeners = false, listeners = {DependencyInjectionTestExecutionListener.class})
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_CLASS)
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

    @Before
    public void clearServletContextCache() {
        AwsServletContext.clearServletContextCache();
    }

    @Test
    public void controllerAdvice_invalidPath_returnAdvice() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo2", "GET")
                                          .json()
                                          .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertNotNull(output);
        assertEquals(404, output.getStatusCode());
        validateSingleValueModel(output, RestControllerAdvice.ERROR_MESSAGE);

    }

    @Test
    public void headers_getHeaders_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/headers", "GET")
                .json()
                .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

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
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

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
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

        validateMapResponseModel(output);
    }

    @Test
    public void queryString_listParameter_expectCorrectLength() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/list-query-string", "GET")
                                          .json()
                                          .queryString("list", "v1,v2,v3")
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "3");
    }

    @Test
    public void queryString_multiParam_expectCorrectValueCount()
            throws IOException {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/multivalue-query-string", "GET")
                                          .json()
                                          .queryString("multiple", "first")
                                          .queryString("multiple", "second")
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);

        assertEquals(2, response.getValues().size());
        assertTrue(response.getValues().containsKey("first"));
        assertTrue(response.getValues().containsKey("second"));
    }

    @Test
    public void dateHeader_notModified_expect304() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/last-modified", "GET")
                                          .json()
                                          .header(
                                                  HttpHeaders.IF_MODIFIED_SINCE,
                                                  DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().minus(1, ChronoUnit.SECONDS))
                                          )
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(304, output.getStatusCode());
        assertEquals("", output.getBody());
    }

    @Test
    public void dateHeader_notModified_expect200() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/last-modified", "GET")
                                          .json()
                                          .header(
                                                  HttpHeaders.IF_MODIFIED_SINCE,
                                                  DateTimeFormatter.RFC_1123_DATE_TIME.format(ZonedDateTime.now().minus(5, ChronoUnit.DAYS))
                                          )
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals(EchoResource.STRING_BODY, output.getBody());
    }

    @Test
    public void authorizer_securityContext_customPrincipalSuccess() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/authorizer-principal", "GET")
                .json()
                .authorizerPrincipal(AUTHORIZER_PRINCIPAL_ID)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type").split(";")[0]);

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
    public void error_unauthenticatedCall_filterStepsRequest() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/status-code", "GET")
                                          .header(UnauthenticatedFilter.HEADER_NAME, "1")
                                          .json()
                                          .queryString("status", "201")
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(401, output.getStatusCode());
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

    @Test
    public void injectBody_populatedResponse_noException() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/request-body", "POST")
                                          .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                                          .body("This is a populated body")
                                          .build();

        AwsProxyResponse response = handler.proxy(request, lambdaContext);
        assertNotNull(response.getBody());
        assertEquals(200, response.getStatusCode());
        try {
            SingleValueModel output = objectMapper.readValue(response.getBody(), SingleValueModel.class);
            assertEquals("true", output.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }

        AwsProxyRequest emptyReq = new AwsProxyRequestBuilder("/echo/request-body", "POST")
                                          .build();
        AwsProxyResponse emptyResp = handler.proxy(emptyReq, lambdaContext);
        try {
            SingleValueModel output = objectMapper.readValue(emptyResp.getBody(), SingleValueModel.class);
            assertEquals(null, output.getValue());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void servletRequestEncoding_acceptEncoding_okStatusCode() {
        SingleValueModel singleValueModel = new SingleValueModel();
        singleValueModel.setValue(CUSTOM_HEADER_VALUE);
        AwsProxyRequest request = null;
        try {
            request = new AwsProxyRequestBuilder("/echo/json-body", "POST")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                    .header(HttpHeaders.ACCEPT_ENCODING, "gzip, deflate")
                    .queryString("status", "200")
                    .body(objectMapper.writeValueAsString(singleValueModel))
                    .build();
        } catch (JsonProcessingException e) {
            fail("Could not serialize object to JSON");
        }

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
    }

    @Test
    public void request_requestURI() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/request-URI", "GET")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "/echo/request-URI");
    }

    @Test
    public void request_requestURL() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/request-url", "GET")
                .scheme("https")
                .serverName("api.myserver.com")
                .stage("prod")
                .build();
        handler.stripBasePath("");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "https://api.myserver.com/echo/request-url");
    }

    @Test
    public void request_encodedPath_returnsDecodedPath() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/encoded-request-uri/Some%20Thing", "GET")
                                          .scheme("https")
                                          .serverName("api.myserver.com")
                                          .stage("prod")
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        validateSingleValueModel(output, "Some Thing");

    }

    @Test
    public void contextPath_generateLink_returnsCorrectPath() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/generate-uri", "GET")
                                          .scheme("https")
                                          .serverName("api.myserver.com")
                                          .stage("prod")
                                          .build();
        LambdaContainerHandler.getContainerConfig().addCustomDomain("api.myserver.com");
        SpringLambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        System.out.println("Response: " + output.getBody());

        String expectedUri = "https://api.myserver.com/prod/echo/encoded-request-uri/" + EchoResource.TEST_GENERATE_URI;

        validateSingleValueModel(output, expectedUri);

        SpringLambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @Test
    public void multipart_getFileName_rerutrnsCorrectFileName()
            throws IOException {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/attachment", "POST")
                                          .formFilePart("testFile", "myFile.txt", "hello".getBytes())
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        System.out.println("Response: " + output.getBody());

        assertEquals("testFile", output.getBody());
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

