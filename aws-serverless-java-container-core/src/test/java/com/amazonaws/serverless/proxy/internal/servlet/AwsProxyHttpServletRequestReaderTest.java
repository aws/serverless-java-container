package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import static org.junit.jupiter.api.Assertions.*;


public class AwsProxyHttpServletRequestReaderTest {
    private AwsProxyHttpServletRequestReader reader = new AwsProxyHttpServletRequestReader();

    private static final String TEST_HEADER_KEY = "x-test";
    private static final String TEST_HEADER_VALUE = "header";
    private static final String ENCODED_REQUEST_PATH = "/foo/bar/Some%20Thing";
    private static final String DECODED_REQUEST_PATH = "/foo/bar/Some Thing";

    @Test
    void readRequest_validAwsProxy_populatedRequest() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/path", "GET").header(TEST_HEADER_KEY, TEST_HEADER_VALUE).build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletRequest.getHeader(TEST_HEADER_KEY));
            assertEquals(TEST_HEADER_VALUE, servletRequest.getHeader(TEST_HEADER_KEY));
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }

    @Test
    void readRequest_urlDecode_expectDecodedPath() {
        AwsProxyRequest request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletRequest);
            assertEquals(DECODED_REQUEST_PATH, servletRequest.getPathInfo());
            assertEquals(ENCODED_REQUEST_PATH, servletRequest.getRequestURI());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }

    }

    @Test
    void readRequest_contentCharset_doesNotOverrideRequestCharset() {
        String requestCharset = "application/json; charset=UTF-8";
        AwsProxyRequest request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, requestCharset).build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletRequest);
            assertNotNull(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(requestCharset, servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals("UTF-8", servletRequest.getCharacterEncoding());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }

    @Test
    void readRequest_contentCharset_setsDefaultCharsetWhenNotSpecified() {
        String requestCharset = "application/json";
        AwsProxyRequest request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, requestCharset).build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletRequest);
            assertNotNull(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            String contentAndCharset = requestCharset + "; charset=" + LambdaContainerHandler.getContainerConfig().getDefaultContentCharset();
            assertEquals(contentAndCharset, servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(LambdaContainerHandler.getContainerConfig().getDefaultContentCharset(), servletRequest.getCharacterEncoding());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }

    @Test
    void readRequest_contentCharset_appendsCharsetToComplextContentType() {
        String contentType = "multipart/form-data; boundary=something";
        AwsProxyRequest request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, contentType).build();
        try {
            HttpServletRequest servletRequest = reader.readRequest(request, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletRequest);
            assertNotNull(servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            String contentAndCharset = contentType + "; charset=" + LambdaContainerHandler.getContainerConfig().getDefaultContentCharset();
            assertEquals(contentAndCharset, servletRequest.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(LambdaContainerHandler.getContainerConfig().getDefaultContentCharset(), servletRequest.getCharacterEncoding());
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }

    @Test
    void readRequest_validEventEmptyPath_expectException() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder(null, "GET").build();
            HttpServletRequest servletReq = reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            assertNotNull(servletReq);
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read a request with a null path");
        }
    }

    @Test
    void readRequest_invalidEventEmptyMethod_expectException() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder("/path", null).build();
            reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            fail("Expected InvalidRequestEventException");
        } catch (InvalidRequestEventException e) {
            assertEquals(AwsProxyHttpServletRequestReader.INVALID_REQUEST_ERROR, e.getMessage());
        }
    }

    @Test
    void readRequest_invalidEventEmptyContext_expectException() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder("/path", "GET").build();
            req.setRequestContext(null);
            reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            fail("Expected InvalidRequestEventException");
        } catch (InvalidRequestEventException e) {
            assertEquals(AwsProxyHttpServletRequestReader.INVALID_REQUEST_ERROR, e.getMessage());
        }
    }

    @Test
    void readRequest_nullHeaders_expectSuccess() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/path", "GET").build();
        req.setMultiValueHeaders(null);
        try {
            HttpServletRequest servletReq = reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            String headerValue = servletReq.getHeader(HttpHeaders.CONTENT_TYPE);
            assertNull(headerValue);
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Failed to read request with null headers");
        }
    }

    @Test
    void readRequest_emptyHeaders_expectSuccess() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/path", "GET").build();
        try {
            HttpServletRequest servletReq = reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            String headerValue = servletReq.getHeader(HttpHeaders.CONTENT_TYPE);
            assertNull(headerValue);
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Failed to read request with null headers");
        }
    }
}
