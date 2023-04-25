package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;

import static org.junit.jupiter.api.Assertions.*;

public class AwsHttpApiV2HttpServletRequestReaderTest {
    private AwsHttpApiV2HttpServletRequestReader reader = new AwsHttpApiV2HttpServletRequestReader();

    @Test
    void reflection_getRequestClass_returnsCorrectType() {
        assertSame(HttpApiV2ProxyRequest.class, reader.getRequestClass());
    }

    @Test
    void baseRequest_read_populatesSuccessfully() {
        HttpApiV2ProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET")
                .referer("localhost")
                .userAgent("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.61 Safari/537.36")
                .queryString("param1", "value1")
                .header("custom", "value")
                .cookie("cookey", "cooval")
                .apiId("test").toHttpApiV2Request();
        AwsHttpApiV2HttpServletRequestReader reader = new AwsHttpApiV2HttpServletRequestReader();
        try {
            HttpServletRequest servletRequest = reader.readRequest(req, null, null, LambdaContainerHandler.getContainerConfig());
            assertEquals("/hello", servletRequest.getPathInfo());
            assertEquals("value1", servletRequest.getParameter("param1"));
            assertEquals("value", servletRequest.getHeader("CUSTOM"));
            assertEquals("Mozilla/5.0 (X11; Linux x86_64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/102.0.5005.61 Safari/537.36", servletRequest.getHeader(HttpHeaders.USER_AGENT));

            assertNotNull(servletRequest.getCookies());
            assertEquals(1, servletRequest.getCookies().length);
            assertEquals("cookey", servletRequest.getCookies()[0].getName());
            assertEquals("cooval", servletRequest.getCookies()[0].getValue());

            assertNotNull(servletRequest.getAttribute(AwsHttpApiV2HttpServletRequestReader.HTTP_API_CONTEXT_PROPERTY));
            assertEquals("test",
                    ((HttpApiV2ProxyRequestContext)servletRequest.getAttribute(AwsHttpApiV2HttpServletRequestReader.HTTP_API_CONTEXT_PROPERTY)).getApiId());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }
}
