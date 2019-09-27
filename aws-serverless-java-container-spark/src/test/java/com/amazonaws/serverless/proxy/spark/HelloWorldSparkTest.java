package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import spark.Spark;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.*;
import static spark.Spark.get;


@RunWith(Parameterized.class)
public class HelloWorldSparkTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "My Header Value";
    private static final String BODY_TEXT_RESPONSE = "Hello World";

    private static final String COOKIE_NAME = "MyCookie";
    private static final String COOKIE_VALUE = "CookieValue";
    private static final String COOKIE_DOMAIN = "mydomain.com";
    private static final String COOKIE_PATH = "/";

    private static final String READ_COOKIE_NAME = "customCookie";

    private static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    private boolean isAlb;

    public HelloWorldSparkTest(boolean alb) {
        isAlb = alb;
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { false, true });
    }

    private AwsProxyRequestBuilder getRequestBuilder() {
        AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder();
        if (isAlb) builder.alb();

        return builder;
    }

    @BeforeClass
    public static void initializeServer() {
        try {
            handler = SparkLambdaContainerHandler.getAwsProxyHandler();

            configureRoutes();
            Spark.awaitInitialization();
        } catch (RuntimeException | ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @AfterClass
    public static void stopSpark() {
        Spark.stop();
    }

    @Test
    public void basicServer_handleRequest_emptyFilters() {
        AwsProxyRequest req = getRequestBuilder().method("GET").path("/hello").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMultiValueHeaders().containsKey(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, response.getMultiValueHeaders().getFirst(CUSTOM_HEADER_KEY));
        assertEquals(BODY_TEXT_RESPONSE, response.getBody());
    }

    @Test
    public void addCookie_setCookieOnResponse_validCustomCookie() {
        AwsProxyRequest req = getRequestBuilder().method("GET").path("/cookie").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMultiValueHeaders().containsKey(HttpHeaders.SET_COOKIE));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_NAME + "=" + COOKIE_VALUE));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_DOMAIN));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_PATH));
    }

    @Test
    public void multiCookie_setCookieOnResponse_singleHeaderWithMultipleValues() {
        AwsProxyRequest req = getRequestBuilder().method("GET").path("/multi-cookie").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMultiValueHeaders().containsKey(HttpHeaders.SET_COOKIE));

        assertEquals(2, response.getMultiValueHeaders().get(HttpHeaders.SET_COOKIE).size());
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_NAME + "=" + COOKIE_VALUE));
        assertTrue(response.getMultiValueHeaders().get(HttpHeaders.SET_COOKIE).get(1).contains(COOKIE_NAME + "2=" + COOKIE_VALUE + "2"));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_DOMAIN));
        assertTrue(response.getMultiValueHeaders().getFirst(HttpHeaders.SET_COOKIE).contains(COOKIE_PATH));
    }

    @Test
    public void rootResource_basicRequest_expectSuccess() {
        AwsProxyRequest req = getRequestBuilder().method("GET").path("/").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMultiValueHeaders().containsKey(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, response.getMultiValueHeaders().getFirst(CUSTOM_HEADER_KEY));
        assertEquals(BODY_TEXT_RESPONSE, response.getBody());
    }

    @Test
    public void readCookie_customDomainName_expectValidCookie() {
        AwsProxyRequest req = getRequestBuilder().method("GET").path("/cookie-read").cookie(READ_COOKIE_NAME, "test").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());
        assertEquals("test", response.getBody());
    }

    private static void configureRoutes() {
        get("/", (req, res) -> {
            res.status(200);
            res.header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE);
            return BODY_TEXT_RESPONSE;
        });

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

        get("/cookie-read", (req, res) -> req.cookie(READ_COOKIE_NAME));
    }
}
