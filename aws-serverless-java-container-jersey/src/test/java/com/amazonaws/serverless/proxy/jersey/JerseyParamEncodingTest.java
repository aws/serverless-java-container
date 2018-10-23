package com.amazonaws.serverless.proxy.jersey;


import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.jersey.model.MapResponseModel;
import com.amazonaws.serverless.proxy.jersey.model.SingleValueModel;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.glassfish.jersey.logging.LoggingFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Ignore;
import org.junit.Test;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class JerseyParamEncodingTest {

    private static final String SIMPLE_ENCODED_PARAM = "p/z+3";
    private static final String JSON_ENCODED_PARAM = "{\"name\":\"faisal\"}";
    private static final String QUERY_STRING_KEY = "identifier";
    private static final String QUERY_STRING_NON_ENCODED_VALUE = "Space Test";
    private static final String QUERY_STRING_ENCODED_VALUE = "Space%20Test";


    private static ObjectMapper objectMapper = new ObjectMapper();
    private static ResourceConfig app = new ResourceConfig().packages("com.amazonaws.serverless.proxy.jersey")
                                                            .register(LoggingFeature.class)
                                                            .property(LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER, LoggingFeature.Verbosity.PAYLOAD_ANY);
    private static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = JerseyLambdaContainerHandler.getAwsProxyHandler(app);

    private static Context lambdaContext = new MockLambdaContext();

    @Test
    public void queryString_uriInfo_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/query-string", "GET")
                                          .json()
                                          .queryString(QUERY_STRING_KEY, QUERY_STRING_NON_ENCODED_VALUE)
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output, QUERY_STRING_KEY, QUERY_STRING_NON_ENCODED_VALUE);
    }

    @Test
    public void queryString_notEncoded_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/query-string", "GET")
                                          .json()
                                          .queryString(QUERY_STRING_KEY, QUERY_STRING_NON_ENCODED_VALUE)
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output, QUERY_STRING_KEY, QUERY_STRING_NON_ENCODED_VALUE);
    }

    @Test
    @Ignore("We expect to only receive decoded values from API Gateway")
    public void queryString_encoded_echo() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/query-string", "GET")
                                          .json()
                                          .queryString(QUERY_STRING_KEY, QUERY_STRING_ENCODED_VALUE)
                                          .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("application/json", output.getMultiValueHeaders().getFirst("Content-Type"));

        validateMapResponseModel(output, QUERY_STRING_KEY, QUERY_STRING_NON_ENCODED_VALUE);
    }

    @Test
    public void simpleQueryParam_encoding_expectDecodedParam() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/decoded-param", "GET").queryString("param", SIMPLE_ENCODED_PARAM).build();

        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, SIMPLE_ENCODED_PARAM);
    }

    @Test
    public void jsonQueryParam_encoding_expectDecodedParam() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/decoded-param", "GET").queryString("param", JSON_ENCODED_PARAM).build();

        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, JSON_ENCODED_PARAM);
    }

    @Test
    public void simpleQueryParam_encoding_expectEncodedParam() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/encoded-param", "GET").queryString("param", SIMPLE_ENCODED_PARAM).build();
        String encodedVal = "";
        try {
            encodedVal = URLEncoder.encode(SIMPLE_ENCODED_PARAM, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fail("Could not encode parameter value");
        }
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, encodedVal);
    }

    @Test
    public void jsonQueryParam_encoding_expectEncodedParam() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/encoded-param", "GET").queryString("param", JSON_ENCODED_PARAM).build();
        String encodedVal = "";
        try {
            encodedVal = URLEncoder.encode(JSON_ENCODED_PARAM, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            fail("Could not encode parameter value");
        }
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertEquals(200, resp.getStatusCode());
        validateSingleValueModel(resp, encodedVal);
    }

    @Test
    public void queryParam_encoding_expectFullyEncodedUrl() {
        String paramValue = "/+=";
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/encoded-param", "GET").queryString("param", paramValue).build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertNotNull(resp);
        assertEquals(resp.getStatusCode(), 200);
        validateSingleValueModel(resp, "%2F%2B%3D");
        System.out.println("body:" + resp.getBody());
    }

    @Test
    public void pathParam_encoded_routesToCorrectPath() {
        String encodedParam = "http%3A%2F%2Fhelloresource.com";
        String path = "/echo/encoded-path/" + encodedParam;
        AwsProxyRequest request = new AwsProxyRequestBuilder(path, "GET").build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertNotNull(resp);
        assertEquals(resp.getStatusCode(), 200);
        validateSingleValueModel(resp, encodedParam);
    }

    @Test
    public void pathParam_encoded_returns404() {
        String encodedParam = "http://helloresource.com";
        String path = "/echo/encoded-path/" + encodedParam;
        AwsProxyRequest request = new AwsProxyRequestBuilder(path, "GET").build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertNotNull(resp);
        assertEquals(resp.getStatusCode(), 404);
    }

    @Test
    @Ignore
    public void queryParam_listOfString_expectCorrectLength() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/list-query-string", "GET").queryString("list", "v1,v2,v3").build();
        AwsProxyResponse resp = handler.proxy(request, lambdaContext);
        assertNotNull(resp);
        assertEquals(resp.getStatusCode(), 200);
        validateSingleValueModel(resp, "3");
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

    private void validateMapResponseModel(AwsProxyResponse output, String key, String value) {
        try {
            MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
            assertNotNull(response.getValues().get(key));
            assertEquals(value, response.getValues().get(key));
        } catch (IOException e) {
            fail("Exception while parsing response body: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
