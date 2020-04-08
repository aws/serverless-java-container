package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.AfterClass;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import spark.Spark;

import javax.servlet.http.Cookie;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.Collection;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static spark.Spark.get;

// This class doesn't actually test Spark. Instead it tests the proxyStream method of the
// LambdaContainerHandler object. We use the Spark implementation for this because it's the
// fastest to start
@RunWith(Parameterized.class)
public class HelloWorldSparkStreamTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "My Header Value";
    private static final String BODY_TEXT_RESPONSE = "Hello World";

    private static final String COOKIE_NAME = "MyCookie";
    private static final String COOKIE_VALUE = "CookieValue";
    private static final String COOKIE_DOMAIN = "mydomain.com";
    private static final String COOKIE_PATH = "/";

    private static SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static SparkLambdaContainerHandler<HttpApiV2ProxyRequest, AwsProxyResponse> httpApiHandler;

    private String type;

    public HelloWorldSparkStreamTest(String reqType) {
        type = reqType;
        try {
            switch (type) {
                case "API_GW":
                case "ALB":
                    handler = SparkLambdaContainerHandler.getAwsProxyHandler();
                    break;
                case "HTTP_API":
                    httpApiHandler = SparkLambdaContainerHandler.getHttpApiV2ProxyHandler();
                    break;
                default:
                    throw new RuntimeException("Unknown request type: " + type);
            }

            configureRoutes();
            Spark.awaitInitialization();
        } catch (RuntimeException | ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { "API_GW", "ALB", "HTTP_API" });
    }

    private AwsProxyRequestBuilder getRequestBuilder() {
        return new AwsProxyRequestBuilder();
    }

    private ByteArrayOutputStream executeRequest(AwsProxyRequestBuilder requestBuilder, Context lambdaContext) throws IOException {
        ByteArrayOutputStream os = new ByteArrayOutputStream();
        switch (type) {
            case "API_GW":
                handler.proxyStream(requestBuilder.buildStream(), os, lambdaContext);
                break;
            case "ALB":
                handler.proxyStream(requestBuilder.alb().buildStream(), os, lambdaContext);
                break;
            case "HTTP_API":
                httpApiHandler.proxyStream(requestBuilder.toHttpApiV2RequestStream(), os, lambdaContext);
                break;
            default:
                throw new RuntimeException("Unknown request type: " + type);
        }
        return os;
    }

    @AfterClass
    public static void stopSpark() {
        Spark.stop();
    }

    @Test
    public void helloRequest_basicStream_populatesOutputSuccessfully() {
        try {
            ByteArrayOutputStream outputStream = executeRequest(getRequestBuilder().method("GET").path("/hello"), new MockLambdaContext());
            AwsProxyResponse response = LambdaContainerHandler.getObjectMapper().readValue(outputStream.toByteArray(), AwsProxyResponse.class);

            assertEquals(200, response.getStatusCode());
            assertTrue(response.getMultiValueHeaders().containsKey(CUSTOM_HEADER_KEY));
            assertEquals(CUSTOM_HEADER_VALUE, response.getMultiValueHeaders().getFirst(CUSTOM_HEADER_KEY));
            assertEquals(BODY_TEXT_RESPONSE, response.getBody());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void nullPathRequest_doesntFail_expectA404() {
        try {
            ByteArrayOutputStream outputStream = executeRequest(getRequestBuilder().method("GET").path(null), new MockLambdaContext());
            AwsProxyResponse response = LambdaContainerHandler.getObjectMapper().readValue(outputStream.toByteArray(), AwsProxyResponse.class);

            assertEquals(404, response.getStatusCode());
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
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
