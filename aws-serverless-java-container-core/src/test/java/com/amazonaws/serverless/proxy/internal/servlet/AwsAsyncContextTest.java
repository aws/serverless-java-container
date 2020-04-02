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
import org.junit.Test;

import javax.servlet.AsyncContext;
import javax.servlet.Servlet;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.concurrent.CountDownLatch;

import static junit.framework.TestCase.assertNotNull;
import static org.junit.Assert.assertEquals;

public class AwsAsyncContextTest {
    private MockLambdaContext lambdaCtx = new MockLambdaContext();
    private MockContainerHandler handler = new MockContainerHandler();
    private AwsProxyHttpServletRequestReader reader = new AwsProxyHttpServletRequestReader();
    private AwsServletContextTest.TestServlet srv1 = new AwsServletContextTest.TestServlet("srv1");
    private AwsServletContextTest.TestServlet srv2 = new AwsServletContextTest.TestServlet("srv2");
    private AwsServletContext ctx = getCtx();

    @Test
    public void dispatch_sendsToCorrectServlet() {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(new AwsProxyRequestBuilder("/srv1/hello", "GET").build(), lambdaCtx, null);
        req.setResponse(handler.getContainerResponse(req, new CountDownLatch(1)));
        req.setServletContext(ctx);
        req.setContainerHandler(handler);

        AsyncContext asyncCtx = req.startAsync();
        handler.setDesiredStatus(201);
        asyncCtx.dispatch();
        assertNotNull(handler.getSelectedServlet());
        assertEquals(srv1, handler.getSelectedServlet());
        assertEquals(201, handler.getResponse().getStatus());

        req = new AwsProxyHttpServletRequest(new AwsProxyRequestBuilder("/srv5/hello", "GET").build(), lambdaCtx, null);
        req.setResponse(handler.getContainerResponse(req, new CountDownLatch(1)));
        req.setServletContext(ctx);
        req.setContainerHandler(handler);
        asyncCtx = req.startAsync();
        handler.setDesiredStatus(202);
        asyncCtx.dispatch();
        assertNotNull(handler.getSelectedServlet());
        assertEquals(srv2, handler.getSelectedServlet());
        assertEquals(202, handler.getResponse().getStatus());
    }

    @Test
    public void dispatchNewPath_sendsToCorrectServlet() throws InvalidRequestEventException {
        AwsProxyHttpServletRequest req = (AwsProxyHttpServletRequest) reader.readRequest(new AwsProxyRequestBuilder("/srv1/hello", "GET").build(), null, lambdaCtx, LambdaContainerHandler.getContainerConfig());
        req.setResponse(handler.getContainerResponse(req, new CountDownLatch(1)));
        req.setServletContext(ctx);
        req.setContainerHandler(handler);

        AsyncContext asyncCtx = req.startAsync();
        handler.setDesiredStatus(301);
        asyncCtx.dispatch("/srv4/hello");
        assertNotNull(handler.getSelectedServlet());
        assertEquals(srv2, handler.getSelectedServlet());
        assertNotNull(handler.getResponse());
        assertEquals(301, handler.getResponse().getStatus());
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
