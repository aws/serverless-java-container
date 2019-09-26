package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class ServletLambdaContainerHandlerBuilderTest {

    @Test
    public void validation_throwsException() {
        TestBuilder testBuilder = new TestBuilder();
        try {
            testBuilder.validate();
        } catch (ContainerInitializationException e) {
            return;
        }
        fail("Did not throw exception");
    }

    @Test
    public void additionalMethod_testSetter() {
        TestBuilder test = new TestBuilder().exceptionHandler(new AwsProxyExceptionHandler()).name("test");
        assertEquals("test", test.getName());
    }

    @Test
    public void defaultProxy_setsValuesCorrectly() {
        TestBuilder test = new TestBuilder().defaultProxy().name("test");
        assertNotNull(test.initializationWrapper);
        assertTrue(test.exceptionHandler instanceof AwsProxyExceptionHandler);
        assertTrue(test.requestReader instanceof AwsProxyHttpServletRequestReader);
        assertTrue(test.responseWriter instanceof AwsProxyHttpServletResponseWriter);
        assertTrue(test.securityContextWriter instanceof AwsProxySecurityContextWriter);
        assertSame(test.requestTypeClass, AwsProxyRequest.class);
        assertSame(test.responseTypeClass, AwsProxyResponse.class);
        assertEquals("test", test.name);
    }

    public static final class TestHandler extends AwsLambdaServletContainerHandler<AwsProxyRequest, AwsProxyResponse, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

        public TestHandler() {
            super(AwsProxyRequest.class, AwsProxyResponse.class, new AwsProxyHttpServletRequestReader(), new AwsProxyHttpServletResponseWriter(), new AwsProxySecurityContextWriter(), new AwsProxyExceptionHandler());
        }
        @Override
        protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
            return null;
        }

        @Override
        protected void handleRequest(AwsProxyHttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {

        }

        @Override
        public void initialize() throws ContainerInitializationException {

        }
    }

    public static final class TestBuilder
            extends ServletLambdaContainerHandlerBuilder<
            AwsProxyRequest,
            AwsProxyResponse,
            AwsProxyHttpServletRequest,
            TestHandler,
            TestBuilder> {

        public TestBuilder() {
            super();
        }

        private String name;

        public TestBuilder name(String n) {
            name = n;
            return this;
        }

        public String getName() {
            return name;
        }

        @Override
        protected TestBuilder self() {
            return this;
        }

        @Override
        public TestHandler build() throws ContainerInitializationException {
            return null;
        }

        @Override
        public TestHandler buildAndInitialize() throws ContainerInitializationException {
            return null;
        }
    }
}
