package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AwsAlbHttpServletRequestReaderTest {
    private AwsAlbHttpServletRequestReader reader = new AwsAlbHttpServletRequestReader();

    private static final String TEST_HEADER_KEY = "x-test";
    private static final String TEST_HEADER_VALUE = "header";
    private static final String ENCODED_REQUEST_PATH = "/foo/bar/Some%20Thing";
    private static final String DECODED_REQUEST_PATH = "/foo/bar/Some Thing";

    @Test
    void readRequest_validAwsProxy_populatedRequest() {
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder("/path", "GET").header(TEST_HEADER_KEY, TEST_HEADER_VALUE).toAlbRequest();
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
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").toAlbRequest();
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
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, requestCharset).toAlbRequest();
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
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, requestCharset).toAlbRequest();
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
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder(ENCODED_REQUEST_PATH, "GET").header(HttpHeaders.CONTENT_TYPE, contentType).toAlbRequest();
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
            ApplicationLoadBalancerRequestEvent req = new AwsProxyRequestBuilder(null, "GET").toAlbRequest();
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
            ApplicationLoadBalancerRequestEvent req = new AwsProxyRequestBuilder("/path", null).toAlbRequest();
            reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            fail("Expected InvalidRequestEventException");
        } catch (InvalidRequestEventException e) {
            assertEquals(AwsAlbHttpServletRequestReader.INVALID_REQUEST_ERROR, e.getMessage());
        }
    }

    @Test
    void readRequest_invalidEventEmptyContext_expectException() {
        try {
            ApplicationLoadBalancerRequestEvent req = new AwsProxyRequestBuilder("/path", "GET").toAlbRequest();
            req.setRequestContext(null);
            reader.readRequest(req, null, null, ContainerConfig.defaultConfig());
            fail("Expected InvalidRequestEventException");
        } catch (InvalidRequestEventException e) {
            assertEquals(AwsAlbHttpServletRequestReader.INVALID_REQUEST_ERROR, e.getMessage());
        }
    }

    @Test
    void readRequest_nullHeaders_expectSuccess() {
        ApplicationLoadBalancerRequestEvent req = new AwsProxyRequestBuilder("/path", "GET").toAlbRequest();
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
        ApplicationLoadBalancerRequestEvent req = new AwsProxyRequestBuilder("/path", "GET").toAlbRequest();
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