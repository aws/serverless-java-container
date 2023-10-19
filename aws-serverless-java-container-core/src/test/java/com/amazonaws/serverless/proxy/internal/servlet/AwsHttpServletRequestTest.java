package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;

import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.Cookie;
import jakarta.ws.rs.core.HttpHeaders;

import static com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest.findKey;
import static org.junit.jupiter.api.Assertions.*;

import java.util.*;


public class AwsHttpServletRequestTest {

    private static final APIGatewayProxyRequestEvent contentTypeRequest = new AwsProxyRequestBuilder("/test", "GET")
            .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=utf-8").build();
    private static final APIGatewayProxyRequestEvent validCookieRequest = new AwsProxyRequestBuilder("/cookie", "GET")
            .header(HttpHeaders.COOKIE, "yummy_cookie=choco; tasty_cookie=strawberry").build();
    private static final APIGatewayProxyRequestEvent complexAcceptHeader = new AwsProxyRequestBuilder("/accept", "GET")
            .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").build();
    private static final APIGatewayProxyRequestEvent queryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("three", "four").build();
    private static final APIGatewayProxyRequestEvent queryStringNullValue = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("three", null).build();
    private static final APIGatewayProxyRequestEvent encodedQueryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("json", "{\"name\":\"faisal\"}").build();
    private static final APIGatewayProxyRequestEvent multipleParams = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("one", "three").queryString("json", "{\"name\":\"faisal\"}").build();
    private static final APIGatewayProxyRequestEvent formEncodedAndQueryString = new AwsProxyRequestBuilder("/test", "POST")
            .queryString("one", "two").queryString("one", "three")
            .queryString("five", "six")
            .form("one", "four")
            .form("seven", "eight").build();
    private static final APIGatewayProxyRequestEvent differentCasing = new AwsProxyRequestBuilder("/test", "POST")
            .queryString("one", "two").queryString("one", "three")
            .queryString("ONE", "four").build();

    private static final MockLambdaContext mockContext = new MockLambdaContext();

    private static ContainerConfig config = ContainerConfig.defaultConfig();

    @Test
    void headers_parseHeaderValue_multiValue() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(contentTypeRequest, mockContext, null, config);
        // I'm also using this to double-check that I can get a header ignoring case
        List<AwsHttpServletRequest.HeaderValue> values = request.parseHeaderValue(request.getHeader("content-type"));

        assertEquals(2, values.size());
        assertEquals("application/xml", values.get(0).getValue());
        assertNull(values.get(0).getKey());

        assertEquals("charset", values.get(1).getKey());
        assertEquals("utf-8", values.get(1).getValue());
    }

    @Test
    void headers_parseHeaderValue_validMultipleCookie() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(validCookieRequest, mockContext, null, config);
        List<AwsHttpServletRequest.HeaderValue> values = request.parseHeaderValue(request.getHeader(HttpHeaders.COOKIE), ";", ",");

        assertEquals(2, values.size());
        assertEquals("yummy_cookie", values.get(0).getKey());
        assertEquals("choco", values.get(0).getValue());
        assertEquals("tasty_cookie", values.get(1).getKey());
        assertEquals("strawberry", values.get(1).getValue());
    }

    @Test
    void headers_parseHeaderValue_complexAccept() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(complexAcceptHeader, mockContext, null, config);
        List<AwsHttpServletRequest.HeaderValue> values = request.parseHeaderValue(request.getHeader(HttpHeaders.ACCEPT), ",", ";");

        assertEquals(4, values.size());
    }

    @Test
    void headers_parseHeaderValue_encodedContentWithEquals() {
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(null, null, null);

        String value = Base64.getUrlEncoder().encodeToString("a".getBytes());

        List<AwsHttpServletRequest.HeaderValue> result = context.parseHeaderValue(value);
        assertTrue(result.size() > 0);
        assertEquals("YQ==", result.get(0).getValue());
    }

    @Test
    void headers_parseHeaderValue_base64EncodedCookieValue() {
        String value = Base64.getUrlEncoder().encodeToString("a".getBytes());
        String cookieValue = "jwt=" + value + "; secondValue=second";
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder("/test", "GET").header(HttpHeaders.COOKIE, cookieValue).build();
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(req, null, null);

        Cookie[] cookies = context.getCookies();

        assertEquals(2, cookies.length);
        assertEquals("jwt", cookies[0].getName());
        assertEquals(value, cookies[0].getValue());
    }

    @Test
    void headers_parseHeaderValue_cookieWithSeparatorInValue() {
        String cookieValue = "jwt==test; secondValue=second";
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder("/test", "GET").header(HttpHeaders.COOKIE, cookieValue).build();
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(req, null, null);

        Cookie[] cookies = context.getCookies();

        assertEquals(2, cookies.length);
        assertEquals("jwt", cookies[0].getName());
        assertEquals("=test", cookies[0].getValue());
    }

    @Test
    void headers_parseHeaderValue_headerWithPaddingButNotBase64Encoded() {
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(null, null, null);

        List<AwsHttpServletRequest.HeaderValue> result = context.parseHeaderValue("hello=");
        assertTrue(result.size() > 0);
        assertEquals("hello", result.get(0).getKey());
        assertNull(result.get(0).getValue());
    }

    @Test
    void queryString_generateQueryString_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryString, mockContext, null, config);

        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }
        assertTrue(parsedString.contains("one=two"));
        assertTrue(parsedString.contains("three=four"));
        assertTrue(parsedString.contains("&") && parsedString.indexOf("&") > 0 && parsedString.indexOf("&") < parsedString.length());
    }

    @Test
    void queryString_generateQueryString_nullParameterIsEmpty() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryStringNullValue, mockContext, null, config);
        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }

        assertTrue(parsedString.endsWith("three="));
    }

    @Test
    void queryStringWithEncodedParams_generateQueryString_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(encodedQueryString, mockContext, null, config);

        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }
        assertTrue(parsedString.contains("one=two"));
        assertTrue(parsedString.contains("json=%7B%22name%22%3A%22faisal%22%7D"));
        assertTrue(parsedString.contains("&") && parsedString.indexOf("&") > 0 && parsedString.indexOf("&") < parsedString.length());
    }

    @Test
    void queryStringWithMultipleValues_generateQueryString_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(multipleParams, mockContext, null, config);

        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }
        assertTrue(parsedString.contains("one=two"));
        assertTrue(parsedString.contains("one=three"));
        assertTrue(parsedString.contains("json=%7B%22name%22%3A%22faisal%22%7D"));
        assertTrue(parsedString.contains("&") && parsedString.indexOf("&") > 0 && parsedString.indexOf("&") < parsedString.length());
    }

    @Test
    void parameterMap_generateParameterMap_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryString, mockContext, null, config);

        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }
        assertArrayEquals(new String[]{"two"}, paramMap.get("one"));
        assertArrayEquals(new String[]{"four"}, paramMap.get("three"));
        assertTrue(paramMap.size() == 2);
    }

    @Test
    void parameterMap_generateParameterMap_nullParameter() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryStringNullValue, mockContext, null, config);
        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }

        assertArrayEquals(new String[]{"two"}, paramMap.get("one"));
        assertArrayEquals(new String[]{null}, paramMap.get("three"));
        assertTrue(paramMap.size() == 2);
    }

    @Test
    void parameterMapWithEncodedParams_generateParameterMap_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(encodedQueryString, mockContext, null, config);

        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }

        assertArrayEquals(new String[]{"two"}, paramMap.get("one"));
        assertArrayEquals(new String[]{"{\"name\":\"faisal\"}"}, paramMap.get("json"));
        assertTrue(paramMap.size() == 2);
    }

    @Test
    void parameterMapWithMultipleValues_generateParameterMap_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(multipleParams, mockContext, null, config);

        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }
        assertArrayEquals(new String[]{"two", "three"}, paramMap.get("one"));
        assertArrayEquals(new String[]{"{\"name\":\"faisal\"}"}, paramMap.get("json"));
        assertTrue(paramMap.size() == 2);
    }

    @Test
    void parameterMap_generateParameterMap_formEncodedAndQueryString() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(formEncodedAndQueryString, mockContext, null, config);

        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }
        // Combines form encoded parameters (one=four) with query string (one=two,three)
        // The order between them is not officially guaranteed (it could be four,two,three or two,three,four)
        // Current implementation gives form encoded parameters first
        assertArrayEquals(new String[]{"four", "two", "three"}, paramMap.get("one"));
        assertArrayEquals(new String[]{"six"}, paramMap.get("five"));
        assertArrayEquals(new String[]{"eight"}, paramMap.get("seven"));
        assertTrue(paramMap.size() == 3);
    }

    @Test
    void parameterMap_generateParameterMap_differentCasing() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(differentCasing, mockContext, null, config);

        Map<String, String[]> paramMap = null;
        try {
            paramMap = request.generateParameterMap(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), config);
        } catch (Exception e) {
            e.printStackTrace();
            fail("Could not generate parameter map");
        }
        // If a parameter is duplicated but with a different casing, it's replaced with only one of them
        assertArrayEquals(paramMap.get("one"), paramMap.get("ONE"));
        assertTrue(paramMap.size() == 2);
    }

    @Test
    void queryParamValues_getQueryParamValues() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(new APIGatewayProxyRequestEvent(), mockContext, null);
        Map<String, List<String>> map = new HashMap<>();
        List<String> values = findKey(map, "test");
        values.add("test");
        values.add("test2");
        String[] result1 = request.getQueryParamValues(map, "test", true);
        assertArrayEquals(new String[]{"test", "test2"}, result1);
        String[] result2 = request.getQueryParamValues(map, "TEST", true);
        assertNull(result2);
    }

    @Test
    void queryParamValues_getQueryParamValues_caseInsensitive() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(new APIGatewayProxyRequestEvent(), mockContext, null);
        Map<String, List<String>> map = new HashMap<>();
        List<String> values = findKey(map, "test");
        values.add("test");
        values.add("test2");
        String[] result1 = request.getQueryParamValues(map, "test", false);
        assertArrayEquals(new String[]{"test", "test2"}, result1);
        String[] result2 = request.getQueryParamValues(map, "TEST", false);
        assertArrayEquals(new String[]{"test", "test2"}, result2);
    }

}
