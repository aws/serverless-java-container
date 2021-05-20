package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;

import static org.junit.Assert.*;

public class AwsHttpApiV2HttpServletRequestReaderTest {
    private AwsHttpApiV2HttpServletRequestReader reader = new AwsHttpApiV2HttpServletRequestReader();

    @Test
    public void reflection_getRequestClass_returnsCorrectType() {
        assertSame(HttpApiV2ProxyRequest.class, reader.getRequestClass());
    }

    @Test
    public void baseRequest_read_populatesSuccessfully() {
        HttpApiV2ProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET")
                .referer("localhost")
                .queryString("param1", "value1")
                .header("custom", "value")
                .cookie("_cookie", "baked")
                .apiId("test").toHttpApiV2Request();
        AwsHttpApiV2HttpServletRequestReader reader = new AwsHttpApiV2HttpServletRequestReader();
        try {
            HttpServletRequest servletRequest = reader.readRequest(req, null, null, LambdaContainerHandler.getContainerConfig());
            assertEquals("/hello", servletRequest.getPathInfo());
            assertEquals("value1", servletRequest.getParameter("param1"));
            assertEquals("value", servletRequest.getHeader("CUSTOM"));
            Cookie[] cookies = servletRequest.getCookies();
            assertEquals(1, cookies.length);
            assertEquals("_cookie", cookies[0].getName());
            assertEquals("baked", cookies[0].getValue());

            assertNotNull(servletRequest.getAttribute(AwsHttpApiV2HttpServletRequestReader.HTTP_API_CONTEXT_PROPERTY));
            assertEquals("test",
                    ((HttpApiV2ProxyRequestContext)servletRequest.getAttribute(AwsHttpApiV2HttpServletRequestReader.HTTP_API_CONTEXT_PROPERTY)).getApiId());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }
}
