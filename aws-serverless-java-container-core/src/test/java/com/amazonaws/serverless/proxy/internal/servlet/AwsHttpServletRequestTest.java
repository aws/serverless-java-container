package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import static org.junit.Assert.*;

import java.util.Base64;
import java.util.List;


public class AwsHttpServletRequestTest {

    private static final AwsProxyRequest contentTypeRequest = new AwsProxyRequestBuilder("/test", "GET")
            .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=utf-8").build();
    private static final AwsProxyRequest validCookieRequest = new AwsProxyRequestBuilder("/cookie", "GET")
            .header(HttpHeaders.COOKIE, "yummy_cookie=choco; tasty_cookie=strawberry").build();
    private static final AwsProxyRequest complexAcceptHeader = new AwsProxyRequestBuilder("/accept", "GET")
            .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").build();
    private static final AwsProxyRequest queryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("three", "four").build();
    private static final AwsProxyRequest queryStringNullValue = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("three", null).build();
    private static final AwsProxyRequest encodedQueryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("json", "{\"name\":\"faisal\"}").build();
    private static final AwsProxyRequest multipleParams = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("one", "three").queryString("json", "{\"name\":\"faisal\"}").build();

    private static final MockLambdaContext mockContext = new MockLambdaContext();

    private static ContainerConfig config = ContainerConfig.defaultConfig();

    @Test
    public void headers_parseHeaderValue_multiValue() {
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
    public void headers_parseHeaderValue_validMultipleCookie() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(validCookieRequest, mockContext, null, config);
        List<AwsHttpServletRequest.HeaderValue> values = request.parseHeaderValue(request.getHeader(HttpHeaders.COOKIE), ";", ",");

        assertEquals(2, values.size());
        assertEquals("yummy_cookie", values.get(0).getKey());
        assertEquals("choco", values.get(0).getValue());
        assertEquals("tasty_cookie", values.get(1).getKey());
        assertEquals("strawberry", values.get(1).getValue());
    }

    @Test
    public void headers_parseHeaderValue_complexAccept() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(complexAcceptHeader, mockContext, null, config);
        List<AwsHttpServletRequest.HeaderValue> values = request.parseHeaderValue(request.getHeader(HttpHeaders.ACCEPT), ",", ";");

        assertEquals(4, values.size());
    }

    @Test
    public void headers_parseHeaderValue_encodedContentWithEquals() {
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(null,null,null);

        String value = Base64.getUrlEncoder().encodeToString("a".getBytes());

        List<AwsHttpServletRequest.HeaderValue> result = context.parseHeaderValue(value);
        assertTrue(result.size() > 0);
        assertEquals("YQ==", result.get(0).getValue());
    }

    @Test
    public void headers_parseHeaderValue_base64EncodedCookieValue() {
        String value = Base64.getUrlEncoder().encodeToString("a".getBytes());
        String cookieValue = "jwt=" + value + "; secondValue=second";
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test", "GET").header(HttpHeaders.COOKIE, cookieValue).build();
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(req,null,null);

        Cookie[] cookies = context.getCookies();

        assertEquals(2, cookies.length);
        assertEquals("jwt", cookies[0].getName());
        assertEquals(value, cookies[0].getValue());
    }

    @Test
    public void headers_parseHeaderValue_cookieWithSeparatorInValue() {
        String cookieValue = "jwt==test; secondValue=second";
        AwsProxyRequest req = new AwsProxyRequestBuilder("/test", "GET").header(HttpHeaders.COOKIE, cookieValue).build();
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(req,null,null);

        Cookie[] cookies = context.getCookies();

        assertEquals(2, cookies.length);
        assertEquals("jwt", cookies[0].getName());
        assertEquals("=test", cookies[0].getValue());
    }

    @Test
    public void headers_parseHeaderValue_headerWithPaddingButNotBase64Encoded() {
        AwsHttpServletRequest context = new AwsProxyHttpServletRequest(null,null,null);

        List<AwsHttpServletRequest.HeaderValue> result = context.parseHeaderValue("hello=");
        assertTrue(result.size() > 0);
        assertEquals("hello", result.get(0).getKey());
        assertNull(result.get(0).getValue());
    }

    @Test
    public void queryString_generateQueryString_validQuery() {
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
    public void queryString_generateQueryString_nullParameterIsEmpty() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryStringNullValue, mockContext, null, config);String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getMultiValueQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }

        assertTrue(parsedString.endsWith("three="));
    }

    @Test
    public void queryStringWithEncodedParams_generateQueryString_validQuery() {
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
    public void queryStringWithMultipleValues_generateQueryString_validQuery() {
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
}
