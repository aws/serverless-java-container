package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.ServletContext;

import static org.junit.Assert.*;

public class AwsFilterChainManagerTest {

    private static AwsFilterChainManager chainManager;
    private static Context lambdaContext = new MockLambdaContext();

    @BeforeClass
    public static void setUp() {
        ServletContext context = AwsServletContext.getInstance(lambdaContext);
        chainManager = new AwsFilterChainManager((AwsServletContext)context);
    }

    @Test
    public void paths_pathMatches_validPaths() {
        assertTrue(chainManager.pathMatches("/users/123123123", "/users/*"));
        assertTrue(chainManager.pathMatches("/apis/123/methods", "/apis/*"));
        assertTrue(chainManager.pathMatches("/very/long/path/with/sub/resources", "/*"));
    }
}
