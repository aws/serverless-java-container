package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.ws.rs.core.HttpHeaders;

import static org.junit.Assert.*;

import java.util.List;
import java.util.Map;


public class AwsHttpServletRequestTest {

    private static final AwsProxyRequest contentTypeRequest = new AwsProxyRequestBuilder("/test", "GET")
            .header(HttpHeaders.CONTENT_TYPE, "application/xml; charset=utf-8").build();
    private static final AwsProxyRequest validCookieRequest = new AwsProxyRequestBuilder("/cookie", "GET")
            .header(HttpHeaders.COOKIE, "yummy_cookie=choco; tasty_cookie=strawberry").build();
    private static final AwsProxyRequest complexAcceptHeader = new AwsProxyRequestBuilder("/accept", "GET")
            .header(HttpHeaders.ACCEPT, "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8").build();
    private static final AwsProxyRequest queryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("three", "four").build();
    private static final AwsProxyRequest encodedQueryString = new AwsProxyRequestBuilder("/test", "GET")
            .queryString("one", "two").queryString("json", "{\"name\":\"faisal\"}").build();

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

        try {
            System.out.println(new ObjectMapper().writeValueAsString(values));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
        }
        assertEquals(4, values.size());
    }

    @Test
    public void queryString_generateQueryString_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(queryString, mockContext, null, config);

        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }
        System.out.println(parsedString);
        assertTrue(parsedString.contains("one=two"));
        assertTrue(parsedString.contains("three=four"));
        assertTrue(parsedString.contains("&") && parsedString.indexOf("&") > 0 && parsedString.indexOf("&") < parsedString.length());
    }

    @Test
    public void queryStringWithEncodedParams_generateQueryString_validQuery() {
        AwsProxyHttpServletRequest request = new AwsProxyHttpServletRequest(encodedQueryString, mockContext, null, config);

        String parsedString = null;
        try {
            parsedString = request.generateQueryString(request.getAwsProxyRequest().getQueryStringParameters(), true, config.getUriEncoding());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Could not generate query string");
        }
        assertTrue(parsedString.contains("one=two"));
        assertTrue(parsedString.contains("json=%7B%22name%22%3A%22faisal%22%7D"));
        assertTrue(parsedString.contains("&") && parsedString.indexOf("&") > 0 && parsedString.indexOf("&") < parsedString.length());
    }
}
