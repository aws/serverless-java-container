package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spark.filter.CustomHeaderFilter;
import com.amazonaws.serverless.proxy.spark.filter.UnauthenticatedFilter;

import org.junit.AfterClass;
import org.junit.Test;
import spark.Spark;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;

import java.util.EnumSet;

import static org.junit.Assert.*;
import static spark.Spark.get;


public class SparkLambdaContainerHandlerTest {
    private static final String RESPONSE_BODY_TEXT = "hello";

    @Test
    public void filters_onStartupMethod_executeFilters() {

        SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = null;
        try {
             handler = SparkLambdaContainerHandler.getAwsProxyHandler();
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }

        handler.onStartup(c -> {
            if (c == null) {
                fail();
            }
            FilterRegistration.Dynamic registration = c.addFilter("CustomHeaderFilter", CustomHeaderFilter.class);
            // update the registration to map to a path
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            // servlet name mappings are disabled and will throw an exception
        });

        configureRoutes();

        Spark.awaitInitialization();

        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/header-filter").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getMultiValueHeaders().containsKey(CustomHeaderFilter.HEADER_NAME));
        assertEquals(CustomHeaderFilter.HEADER_VALUE, response.getMultiValueHeaders().getFirst(CustomHeaderFilter.HEADER_NAME));
        assertEquals(RESPONSE_BODY_TEXT, response.getBody());

    }

    @Test
    public void filters_unauthenticatedFilter_stopRequestProcessing() {

        SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = null;
        try {
            handler = SparkLambdaContainerHandler.getAwsProxyHandler();
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }

        handler.onStartup(c -> {
            if (c == null) {
                fail();
            }
            FilterRegistration.Dynamic registration = c.addFilter("UnauthenticatedFilter", UnauthenticatedFilter.class);
            // update the registration to map to a path
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/unauth");
            // servlet name mappings are disabled and will throw an exception
        });

        configureRoutes();
        Spark.awaitInitialization();

        // first we test without the custom header, we expect request processing to complete
        // successfully
        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/unauth").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertEquals(RESPONSE_BODY_TEXT, response.getBody());

        // now we test with the custom header, this should stop request processing in the
        // filter and return an unauthenticated response
        AwsProxyRequest unauthReq = new AwsProxyRequestBuilder().method("GET").path("/unauth")
                                                          .header(UnauthenticatedFilter.HEADER_NAME, "1").build();
        AwsProxyResponse unauthResp = handler.proxy(unauthReq, new MockLambdaContext());

        assertNotNull(unauthResp);
        assertEquals(UnauthenticatedFilter.RESPONSE_STATUS, unauthResp.getStatusCode());
        assertEquals("", unauthResp.getBody());
    }

    @AfterClass
    public static void stopSpark() {
        Spark.stop();
    }

    private static void configureRoutes() {
        get("/header-filter", (req, res) -> {
            res.status(200);
            return RESPONSE_BODY_TEXT;
        });

        get("/unauth", (req, res) -> {
           res.status(200);
           return RESPONSE_BODY_TEXT;
        });
    }
}
