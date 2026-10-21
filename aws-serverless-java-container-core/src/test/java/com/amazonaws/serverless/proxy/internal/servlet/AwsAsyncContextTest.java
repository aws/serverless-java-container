package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;

import jakarta.servlet.AsyncContext;
import jakarta.servlet.DispatcherType;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

public class AwsAsyncContextTest {
    private MockLambdaContext lambdaCtx = new MockLambdaContext();
    private MockContainerHandler handler = new MockContainerHandler();
    private AwsProxyHttpServletRequestReader reader = new AwsProxyHttpServletRequestReader();
    private AwsServletContextTest.TestServlet srv1 = new AwsServletContextTest.TestServlet("srv1");
    private AwsServletContextTest.TestServlet srv2 = new AwsServletContextTest.TestServlet("srv2");
    private AwsServletContext ctx = getCtx();


    @Test
    void dispatch_amendsPath() throws InvalidRequestEventException {
        AwsProxyHttpServletRequest req = (AwsProxyHttpServletRequest)reader.readRequest(new AwsProxyRequestBuilder("/srv1/hello", "GET").build(), null, lambdaCtx, LambdaContainerHandler.getContainerConfig());
        req.setResponse(handler.getContainerResponse(req, new CountDownLatch(1)));
        req.setServletContext(ctx);
        req.setContainerHandler(handler);

        AsyncContext asyncCtx = req.startAsync();
        asyncCtx.dispatch("/srv4/hello");
        assertEquals("/srv1/hello", req.getRequestURI());
    }

    @Test
    void doFilter_asyncDispatchWritesWithoutFlush_flushesResponse() throws Exception {
        PassThroughContainerHandler passThroughHandler = new PassThroughContainerHandler();
        AsyncDispatchServlet servlet = new AsyncDispatchServlet(false);
        AwsProxyHttpServletRequest req = asyncDispatchRequest(passThroughHandler, servlet);
        CountDownLatch latch = new CountDownLatch(1);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(req, latch);
        req.setResponse(resp);

        passThroughHandler.doFilter(req, resp, servlet);

        assertTrue(resp.isCommitted());
        assertEquals(0, latch.getCount());
        assertEquals("hello", resp.getAwsResponseBodyString());
    }

    @Test
    void doFilter_asyncDispatchStartsNewAsync_leavesResponseOpen() throws Exception {
        PassThroughContainerHandler passThroughHandler = new PassThroughContainerHandler();
        AsyncDispatchServlet servlet = new AsyncDispatchServlet(true);
        AwsProxyHttpServletRequest req = asyncDispatchRequest(passThroughHandler, servlet);
        CountDownLatch latch = new CountDownLatch(1);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(req, latch);
        req.setResponse(resp);

        passThroughHandler.doFilter(req, resp, servlet);

        assertFalse(resp.isCommitted());
        assertEquals(1, latch.getCount());
    }

    private AwsProxyHttpServletRequest asyncDispatchRequest(PassThroughContainerHandler passThroughHandler, Servlet servlet) throws InvalidRequestEventException {
        AwsServletContext servletCtx = new AwsServletContext(passThroughHandler);
        passThroughHandler.setServletContext(servletCtx);
        servletCtx.addServlet("async", servlet).addMapping("/async");

        AwsProxyHttpServletRequest req = (AwsProxyHttpServletRequest)reader.readRequest(new AwsProxyRequestBuilder("/async", "GET").build(), null, lambdaCtx, LambdaContainerHandler.getContainerConfig());
        req.setServletContext(servletCtx);
        return req;
    }


    private AwsServletContext getCtx() {
        AwsServletContext ctx = new AwsServletContext(handler);
        handler.setServletContext(ctx);

        ServletRegistration.Dynamic reg1 = ctx.addServlet("srv1", srv1);
        reg1.addMapping("/srv1");

        ServletRegistration.Dynamic reg2 = ctx.addServlet("srv2", srv2);
        reg2.addMapping("/");
        return ctx;
    }

    /**
     * Behaves like Spring MVC 7.0.6+ with a result that's ready right away: starts async and dispatches during the
     * first pass, then on the async dispatch writes the body without flushing it.
     */
    private static class AsyncDispatchServlet extends HttpServlet {
        private final boolean startAsyncOnDispatch;

        AsyncDispatchServlet(boolean startAsyncOnDispatch) {
            this.startAsyncOnDispatch = startAsyncOnDispatch;
        }

        @Override
        protected void service(HttpServletRequest req, HttpServletResponse resp) throws IOException {
            if (req.getDispatcherType() != DispatcherType.ASYNC) {
                req.startAsync().dispatch();
            } else if (startAsyncOnDispatch) {
                req.startAsync();
            } else {
                resp.setStatus(200);
                resp.getOutputStream().write("hello".getBytes(StandardCharsets.UTF_8));
            }
        }
    }

    /**
     * Uses the real doFilter from AwsLambdaServletContainerHandler, unlike MockContainerHandler.
     */
    public static class PassThroughContainerHandler extends AwsLambdaServletContainerHandler<AwsProxyRequest, AwsProxyResponse, HttpServletRequest, AwsHttpServletResponse> {
        public PassThroughContainerHandler() {
            super(AwsProxyRequest.class, AwsProxyResponse.class, new AwsProxyHttpServletRequestReader(), new AwsProxyHttpServletResponseWriter(), new AwsProxySecurityContextWriter(), new AwsProxyExceptionHandler());
        }

        @Override
        protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
            return new AwsHttpServletResponse(request, latch);
        }

        @Override
        protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {

        }

        @Override
        public void initialize() throws ContainerInitializationException {

        }
    }

    public static class MockContainerHandler extends AwsLambdaServletContainerHandler<AwsProxyRequest, AwsProxyResponse, HttpServletRequest, AwsHttpServletResponse> {
        private int desiredStatus;
        private HttpServletResponse response;
        private Servlet selectedServlet;

        public MockContainerHandler() {
            super(AwsProxyRequest.class, AwsProxyResponse.class, new AwsProxyHttpServletRequestReader(), new AwsProxyHttpServletResponseWriter(), new AwsProxySecurityContextWriter(), new AwsProxyExceptionHandler());
            desiredStatus = 200;
        }

        @Override
        protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
            return new AwsHttpServletResponse(request, latch);
        }

        @Override
        protected void doFilter(HttpServletRequest request, HttpServletResponse response, Servlet servlet) throws IOException, ServletException {
            selectedServlet = servlet;
            try {
                this.response = response;
                //handleRequest((AwsProxyHttpServletRequest)request, , new MockLambdaContext());
                if (AwsProxyHttpServletRequest.class.isAssignableFrom(request.getClass())) {
                    ((AwsProxyHttpServletRequest)request).setResponse((AwsHttpServletResponse)this.response);
                }
                this.response.setStatus(desiredStatus);
                this.response.flushBuffer();
            } catch (Exception e) {
                throw new ServletException(e);
            }
        }

        @Override
        protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {

        }

        @Override
        public void initialize() throws ContainerInitializationException {

        }

        public void setDesiredStatus(int status) {
            desiredStatus = status;
        }

        public AwsHttpServletResponse getResponse() {
            return (AwsHttpServletResponse)response;
        }

        public Servlet getSelectedServlet() {
            return selectedServlet;
        }
    }
}
