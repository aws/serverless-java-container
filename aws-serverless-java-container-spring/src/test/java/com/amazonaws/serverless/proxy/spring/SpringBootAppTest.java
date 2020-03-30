package com.amazonaws.serverless.proxy.spring;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import com.amazonaws.serverless.proxy.spring.springbootapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.springbootapp.TestController;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.core.SpringVersion;

import javax.ws.rs.core.HttpHeaders;
import java.io.IOException;
import java.util.Objects;

import static com.amazonaws.serverless.proxy.spring.springbootapp.TestController.CUSTOM_HEADER_NAME;
import static com.amazonaws.serverless.proxy.spring.springbootapp.TestController.CUSTOM_QS_NAME;
import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;


public class SpringBootAppTest {
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
    public void testMethod_springSecurity_doesNotThrowException() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, TestController.TEST_VALUE);
    }

    @Test
    public void testMethod_testRequestFromString_doesNotThrowNpe() throws IOException {
        AwsProxyRequest req = new AwsProxyRequestBuilder().fromJsonString("{\n" +
                "  \"resource\": \"/missing-params\",\n" +
                "  \"path\": \"/missing-params\",\n" +
                "  \"httpMethod\": \"GET\",\n" +
                "  \"headers\": null,\n" +
                "  \"multiValueHeaders\": null,\n" +
                "  \"queryStringParameters\": null,\n" +
                "  \"multiValueQueryStringParameters\": null,\n" +
                "  \"pathParameters\": null,\n" +
                "  \"stageVariables\": null,\n" +
                "  \"requestContext\": {\n" +
                "    \"resourcePath\": \"/path/resource\",\n" +
                "    \"httpMethod\": \"POST\",\n" +
                "    \"path\": \"//path/resource\",\n" +
                "    \"accountId\": \"accountIdNumber\",\n" +
                "    \"protocol\": \"HTTP/1.1\",\n" +
                "    \"stage\": \"test-invoke-stage\",\n" +
                "    \"domainPrefix\": \"testPrefix\",\n" +
                "    \"identity\": {\n" +
                "      \"cognitoIdentityPoolId\": null,\n" +
                "      \"cognitoIdentityId\": null,\n" +
                "      \"apiKey\": \"test-invoke-api-key\",\n" +
                "      \"principalOrgId\": null,\n" +
                "      \"cognitoAuthenticationType\": null,\n" +
                "      \"userArn\": \"actual arn\",\n" +
                "      \"apiKeyId\": \"test-invoke-api-key-id\"\n" +
                "    }\n" +
                "  },\n" +
                "  \"body\": \"{ \\\"Key1\\\": \\\"Value1\\\", \\\"Key2\\\": \\\"Value2\\\", \\\"Key3\\\": \\\"Vaue3\\\" }\",\n" +
                "  \"isBase64Encoded\": \"false\"\n" +
                "}").build();

        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        // Spring identifies the missing header
        assertEquals(400, resp.getStatusCode());
        req.setMultiValueHeaders(new Headers());
        req.getMultiValueHeaders().add(CUSTOM_HEADER_NAME, "val");
        resp = handler.handleRequest(req, context);
        assertEquals(400, resp.getStatusCode());
        req.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        req.getMultiValueQueryStringParameters().add(CUSTOM_QS_NAME, "val");
        resp = handler.handleRequest(req, context);
        assertEquals(200, resp.getStatusCode());
    }

    @Test
    public void defaultError_requestForward_springBootForwardsToDefaultErrorPage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test2", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(404, resp.getStatusCode());
        assertNotNull(resp.getMultiValueHeaders());
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

    @Test
    public void queryString_multipleParamsWithSameName_expectUnmarshalAsList() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test/query-string", "GET")
                .queryString("list", "v1").queryString("list", "v2").build();
        AwsProxyResponse resp = handler.handleRequest(req, context);
        assertNotNull(resp);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, "2");
    }

    @Test
    public void staticContent_getHtmlFile_returnsHtmlContent() {
        LambdaContainerHandler.getContainerConfig().addValidFilePath("/Users/bulianis/workspace/aws-serverless-java-container/aws-serverless-java-container-spring");
        AwsProxyRequest request = new AwsProxyRequestBuilder("/static.html", "GET")
                .header(HttpHeaders.ACCEPT, "text/html")
                .header(HttpHeaders.CONTENT_TYPE, "text/plain")
                .build();
        AwsProxyResponse output = handler.handleRequest(request, context);
        assertEquals(200, output.getStatusCode());
        assertTrue(output.getBody().contains("<h1>Static</h1>"));
    }

    @Test
    public void utf8_returnUtf8String_expectCorrectHeaderMediaAndCharset() {
        LambdaContainerHandler.getContainerConfig().setDefaultContentCharset("UTF-8");
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test/utf8", "GET")
                .build();
        AwsProxyResponse output = handler.handleRequest(request, context);
        validateSingleValueModel(output, TestController.UTF8_TEST_STRING);
        assertTrue(output.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertTrue(output.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains(";"));
        assertTrue(output.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("charset=UTF-8"));
    }

    @Test
    public void utf8_returnUtf8String_expectCorrectHeaderMediaAndCharsetNoDefault() {

        AwsProxyRequest request = new AwsProxyRequestBuilder("/test/utf8", "GET")
                .header("Content-Type", "application/json; charset=UTF-8")
                .build();
        AwsProxyResponse output = handler.handleRequest(request, context);
        validateSingleValueModel(output, TestController.UTF8_TEST_STRING);
        assertTrue(output.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertTrue(output.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains(";"));
        assertTrue(output.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).contains("charset=UTF-8"));
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
