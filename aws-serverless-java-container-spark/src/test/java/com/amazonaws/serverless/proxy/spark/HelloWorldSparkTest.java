package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;

import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import static org.junit.Assert.*;
import static spark.Spark.get;


public class HelloWorldSparkTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "My Header Value";
    private static final String BODY_TEXT_RESPONSE = "Hello World";

    public static final String COOKIE_NAME = "MyCookie";
    public static final String COOKIE_VALUE = "CookieValue";
    public static final String COOKIE_DOMAIN = "mydomain.com";
    public static final String COOKIE_PATH = "/";

    private static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @BeforeClass
    public static void initializeServer() {
        try {
            handler = SparkLambdaContainerHandler.getAwsProxyHandler();

            configureRoutes();

        } catch (RuntimeException | ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void basicServer_handleRequest_emptyFilters() {
        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/hello").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, response.getHeaders().get(CUSTOM_HEADER_KEY));
        assertEquals(BODY_TEXT_RESPONSE, response.getBody());
    }

    @Test
    public void addCookie_setCookieOnResponse_validCustomCookie() {
        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/cookie").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_NAME + "=" + COOKIE_VALUE));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_DOMAIN));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_PATH));
    }

    @Test
    public void multiCookie_setCookieOnResponse_singleHeaderWithMultipleValues() {
        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/multi-cookie").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        System.out.println("Cookie: " + response.getHeaders().get(HttpHeaders.SET_COOKIE));

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(HttpHeaders.SET_COOKIE));
        // we compare against 4 because the expiration date of the cookies will also contain a string
        assertEquals(2, response.getHeaders().get(HttpHeaders.SET_COOKIE).split(",").length);
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_NAME + "=" + COOKIE_VALUE));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_NAME + "2=" + COOKIE_VALUE + "2"));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_DOMAIN));
        assertTrue(response.getHeaders().get(HttpHeaders.SET_COOKIE).contains(COOKIE_PATH));
    }

    private static void configureRoutes() {
        get("/hello", (req, res) -> {
            res.status(200);
            res.header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
            return BODY_TEXT_RESPONSE;
        });

        get("/cookie", (req, res) -> {
            Cookie testCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
            testCookie.setDomain(COOKIE_DOMAIN);
            testCookie.setPath(COOKIE_PATH);
            res.raw().addCookie(testCookie);
            return BODY_TEXT_RESPONSE;
        });

        get("/multi-cookie", (req, res) -> {
            Cookie testCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
            testCookie.setDomain(COOKIE_DOMAIN);
            testCookie.setPath(COOKIE_PATH);
            Cookie testCookie2 = new Cookie(COOKIE_NAME + "2", COOKIE_VALUE + "2");
            testCookie2.setDomain(COOKIE_DOMAIN);
            testCookie2.setPath(COOKIE_PATH);
            res.raw().addCookie(testCookie);
            res.raw().addCookie(testCookie2);
            return BODY_TEXT_RESPONSE;
        });
    }
}
