package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.*;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class AwsProxyRequestDispatcherTest {
    public static final String FORWARD_PATH = "/newpath";
    static AwsProxyHttpServletRequestReader requestReader = new AwsProxyHttpServletRequestReader();


    @Test
    public void setPath_forwardByPath_proxyRequestObjectInPropertyReferencesSameProxyRequest() throws InvalidRequestEventException {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest,null, new MockLambdaContext(), ContainerConfig.defaultConfig());

        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        dispatcher.setRequestPath(servletRequest, FORWARD_PATH);
        assertEquals(FORWARD_PATH, servletRequest.getRequestURI());
    }

    @Test
    public void setPathForWrappedRequest_forwardByPath_proxyRequestObjectInPropertyReferencesSameProxyRequest() throws InvalidRequestEventException {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest,null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        SecurityContextHolderAwareRequestWrapper springSecurityRequest = new SecurityContextHolderAwareRequestWrapper(servletRequest, "ADMIN");

        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        dispatcher.setRequestPath(springSecurityRequest, FORWARD_PATH);
        assertEquals(FORWARD_PATH, springSecurityRequest.getRequestURI());
    }

    @Test
    public void setPathForWrappedRequestWithoutGatewayEvent_forwardByPath_throwsException() {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        AwsProxyHttpServletRequest servletRequest = new AwsProxyHttpServletRequest(proxyRequest, new MockLambdaContext(), null);
        SecurityContextHolderAwareRequestWrapper springSecurityRequest = new SecurityContextHolderAwareRequestWrapper(servletRequest, "ADMIN");

        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        try {
            dispatcher.setRequestPath(springSecurityRequest, FORWARD_PATH);
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            return;
        }
        fail();
    }

    @Test
    public void forwardRequest_nullHandler_throwsIllegalStateException() throws InvalidRequestEventException {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest,null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        try {
            dispatcher.forward(servletRequest, new AwsHttpServletResponse(servletRequest, new CountDownLatch(1)));
        } catch (ServletException e) {
            fail("Unexpected ServletException");
        } catch (IOException e) {
            fail("Unexpected IOException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            return;
        }
        fail();
    }

    @Test
    public void forwardRequest_committedResponse_throwsIllegalStateException() throws InvalidRequestEventException {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest,null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, mockLambdaHandler(null));
        AwsHttpServletResponse resp = new AwsHttpServletResponse(servletRequest, new CountDownLatch(1));

        try {
            resp.flushBuffer();
            dispatcher.forward(servletRequest, resp);
        } catch (ServletException e) {
            fail("Unexpected ServletException");
        } catch (IOException e) {
            fail("Unexpected IOException");
        } catch (Exception e) {
            assertTrue(e instanceof IllegalStateException);
            return;
        }
        fail();
    }

    @Test
    public void forwardRequest_partiallyWrittenResponse_resetsBuffer() throws InvalidRequestEventException {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest,null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, mockLambdaHandler(null));
        AwsHttpServletResponse resp = new AwsHttpServletResponse(servletRequest, new CountDownLatch(1));

        try {
            resp.getOutputStream().write("this is a test write".getBytes());
            assertEquals("this is a test write", new String(resp.getAwsResponseBodyBytes(), Charset.defaultCharset()));
            dispatcher.forward(servletRequest, resp);
            assertEquals(0, resp.getAwsResponseBodyBytes().length);

        } catch (ServletException e) {
            fail("Unexpected ServletException");
        } catch (IOException e) {
            fail("Unexpected IOException");
        }
    }

    @Test
    public void include_addsToResponse_appendsCorrectly() throws InvalidRequestEventException, IOException {
        final String firstPart = "first";
        final String secondPart = "second";
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();

        AwsProxyResponse resp = mockLambdaHandler((AwsProxyHttpServletRequest req, AwsHttpServletResponse res)-> {
            if (req.getAttribute("cnt") == null) {
                res.getOutputStream().write(firstPart.getBytes());
                req.setAttribute("cnt", 1);
                req.getRequestDispatcher("/includer").include(req, res);
                res.setStatus(200);
                res.flushBuffer();
            } else {
                res.getOutputStream().write(secondPart.getBytes());
            }
        }).proxy(proxyRequest, new MockLambdaContext());
        assertEquals(firstPart + secondPart, resp.getBody());
    }

    @Test
    public void include_appendsNewHeader_cannotAppendNewHeaders() throws InvalidRequestEventException, IOException {
        final String firstPart = "first";
        final String secondPart = "second";
        final String headerKey = "X-Custom-Header";
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();

        AwsProxyResponse resp = mockLambdaHandler((AwsProxyHttpServletRequest req, AwsHttpServletResponse res)-> {
            if (req.getAttribute("cnt") == null) {
                res.getOutputStream().write(firstPart.getBytes());
                req.setAttribute("cnt", 1);
                req.getRequestDispatcher("/includer").include(req, res);
                res.setStatus(200);
                res.flushBuffer();
            } else {
                res.getOutputStream().write(secondPart.getBytes());
                res.addHeader(headerKey, "value");
            }
        }).proxy(proxyRequest, new MockLambdaContext());
        assertEquals(firstPart + secondPart, resp.getBody());
        assertFalse(resp.getMultiValueHeaders().containsKey(headerKey));
    }

    private interface RequestHandler {
        void handleRequest(AwsProxyHttpServletRequest req, AwsHttpServletResponse resp) throws ServletException, IOException;
    }


    private AwsLambdaServletContainerHandler<AwsProxyRequest, AwsProxyResponse, HttpServletRequest, AwsHttpServletResponse> mockLambdaHandler(RequestHandler h) {
        return new AwsLambdaServletContainerHandler<AwsProxyRequest, AwsProxyResponse, HttpServletRequest, AwsHttpServletResponse>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler()
        ) {

            @Override
            protected void doFilter(HttpServletRequest request, HttpServletResponse response, Servlet servlet) throws IOException, ServletException {
                if (h != null) {
                    h.handleRequest((AwsProxyHttpServletRequest)request, (AwsHttpServletResponse)response);
                }
            }

            @Override
            protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
                return new AwsHttpServletResponse(request, latch);
            }

            @Override
            protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
                if (h != null) {

                    setServletContext(new AwsServletContext(this));
                    ((AwsHttpServletRequest)containerRequest).setServletContext(getServletContext());

                    h.handleRequest((AwsProxyHttpServletRequest)containerRequest, containerResponse);
                }
                containerResponse.flushBuffer();
            }

            @Override
            public void initialize() throws ContainerInitializationException {

            }
        };
    }
}
