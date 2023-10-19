package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;
import org.springframework.security.web.servletapi.SecurityContextHolderAwareRequestWrapper;

import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.Charset;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AwsProxyRequestDispatcherTest {
    public static final String FORWARD_PATH = "/newpath";
    static AwsProxyHttpServletRequestReader requestReader = new AwsProxyHttpServletRequestReader();


    @Test
    void setPath_forwardByPath_proxyRequestObjectInPropertyReferencesSameProxyRequest() throws InvalidRequestEventException {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());

        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        dispatcher.setRequestPath(servletRequest, FORWARD_PATH);
        assertEquals(FORWARD_PATH, servletRequest.getRequestURI());
    }

    @Test
    void setPathForWrappedRequest_forwardByPath_proxyRequestObjectInPropertyReferencesSameProxyRequest() throws InvalidRequestEventException {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        SecurityContextHolderAwareRequestWrapper springSecurityRequest = new SecurityContextHolderAwareRequestWrapper(servletRequest, "ADMIN");

        AwsProxyRequestDispatcher dispatcher = new AwsProxyRequestDispatcher(FORWARD_PATH, false, null);
        dispatcher.setRequestPath(springSecurityRequest, FORWARD_PATH);
        assertEquals(FORWARD_PATH, springSecurityRequest.getRequestURI());
    }

    @Test
    void setPathForWrappedRequestWithoutGatewayEvent_forwardByPath_throwsException() {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
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
    void forwardRequest_nullHandler_throwsIllegalStateException() throws InvalidRequestEventException {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
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
    void forwardRequest_committedResponse_throwsIllegalStateException() throws InvalidRequestEventException {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
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
    void forwardRequest_partiallyWrittenResponse_resetsBuffer() throws InvalidRequestEventException {
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest servletRequest = requestReader.readRequest(proxyRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
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
    void include_addsToResponse_appendsCorrectly() throws InvalidRequestEventException, IOException {
        final String firstPart = "first";
        final String secondPart = "second";
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();

        AwsProxyResponseEvent resp = mockLambdaHandler((AwsProxyHttpServletRequest req, AwsHttpServletResponse res) -> {
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
    void include_appendsNewHeader_cannotAppendNewHeaders() throws InvalidRequestEventException, IOException {
        final String firstPart = "first";
        final String secondPart = "second";
        final String headerKey = "X-Custom-Header";
        APIGatewayProxyRequestEvent proxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();

        AwsProxyResponseEvent resp = mockLambdaHandler((AwsProxyHttpServletRequest req, AwsHttpServletResponse res) -> {
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


    private AwsLambdaServletContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent, HttpServletRequest, AwsHttpServletResponse> mockLambdaHandler(RequestHandler h) {
        return new AwsLambdaServletContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponseEvent, HttpServletRequest, AwsHttpServletResponse>(
                APIGatewayProxyRequestEvent.class,
                AwsProxyResponseEvent.class,
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
