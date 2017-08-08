package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.servlet.filters.UrlPathValidator;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;

import static org.junit.Assert.*;

public class FilterHolderTest {
    private static Context lambdaContext = new MockLambdaContext();

    @Test
    public void annotation_filterRegistration_pathValidator() {
        FilterHolder holder = new FilterHolder(new UrlPathValidator(), new AwsServletContext(null));

        assertTrue(holder.isAnnotated());
        assertNotEquals(UrlPathValidator.class.getName(), holder.getRegistration().getName());
        assertEquals("UrlPathValidator", holder.getFilterName());
        assertEquals("UrlPathValidator", holder.getRegistration().getName());
        assertEquals(1, holder.getRegistration().getUrlPatternMappings().size());
        assertEquals("/*", holder.getRegistration().getUrlPatternMappings().toArray()[0]);
    }
}
