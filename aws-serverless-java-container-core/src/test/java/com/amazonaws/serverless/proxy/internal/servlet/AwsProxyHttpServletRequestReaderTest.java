package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;

import static org.junit.Assert.*;


public class AwsProxyHttpServletRequestReaderTest {
    private AwsProxyHttpServletRequestReader reader = new AwsProxyHttpServletRequestReader();

    private static final String TEST_HEADER_KEY = "x-test";
    private static final String TEST_HEADER_VALUE = "header";

    @Test
    public void readRequest_reflection_returnType() throws NoSuchMethodException {
        Method readRequestMethod = AwsProxyHttpServletRequestReader.class.getMethod("readRequest", AwsProxyRequest.class, SecurityContext.class, Context.class);

        assertTrue(readRequestMethod.getReturnType() == AwsProxyHttpServletRequest.class);
    }

    @Test
    public void readRequest_validAwsProxy_populatedRequest() {
        AwsProxyRequest request = new AwsProxyRequestBuilder().header(TEST_HEADER_KEY, TEST_HEADER_VALUE).build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null);
            assertNotNull(servletRequest.getHeader(TEST_HEADER_KEY));
            assertEquals(TEST_HEADER_VALUE, servletRequest.getHeader(TEST_HEADER_KEY));
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }
}
