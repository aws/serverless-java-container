package com.amazonaws.serverless.proxy.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spark.filter.CustomHeaderFilter;

import org.junit.AfterClass;
import org.junit.BeforeClass;
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
                System.out.println("Null servlet context");
                fail();
            }
            FilterRegistration.Dynamic registration = c.addFilter("CustomHeaderFilter", CustomHeaderFilter.class);
            // update the registration to map to a path
            registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
            // servlet name mappings are disabled and will throw an exception
        });

        configureRoutes();

        AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/header-filter").build();
        AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());

        assertNotNull(response);
        assertEquals(200, response.getStatusCode());
        assertTrue(response.getHeaders().containsKey(CustomHeaderFilter.HEADER_NAME));
        assertEquals(CustomHeaderFilter.HEADER_VALUE, response.getHeaders().get(CustomHeaderFilter.HEADER_NAME));
        assertEquals(RESPONSE_BODY_TEXT, response.getBody());

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
    }
}
