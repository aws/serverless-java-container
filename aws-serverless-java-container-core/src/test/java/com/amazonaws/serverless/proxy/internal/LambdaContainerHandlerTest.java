package com.amazonaws.serverless.proxy.internal;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.hc.client5.http.impl.classic.RequestAbortedException;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.Test;

import jakarta.servlet.http.HttpServletRequest;
import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class LambdaContainerHandlerTest {
    private boolean isRuntimeException = false;
    private boolean throwException = false;

    ExceptionContainerHandlerTest handler = new ExceptionContainerHandlerTest(
            APIGatewayProxyRequestEvent.class, APIGatewayProxyResponseEvent.class,
            new AwsProxyHttpServletRequestReader(), new AwsProxyHttpServletResponseWriter(),
            new AwsProxySecurityContextWriter(), new AwsProxyExceptionHandler(), new InitializationWrapper()
    );

    @Test
    void throwRuntime_returnsUnwrappedException() {
        try {
            isRuntimeException = true;
            throwException = true;
            LambdaContainerHandler.getContainerConfig().setDisableExceptionMapper(true);
            handler.proxy(new AwsProxyRequestBuilder("/test", "GET").build(), new MockLambdaContext());
        } catch (Exception e) {
            assertNotNull(e);
            assertEquals(ExceptionContainerHandlerTest.RUNTIME_MESSAGE, e.getMessage());
            return;
        }
        fail("Did not throw runtime exception");
    }

    @Test
    void throwNonRuntime_returnsWrappedException() {
        try {
            isRuntimeException = false;
            throwException = true;
            LambdaContainerHandler.getContainerConfig().setDisableExceptionMapper(true);
            handler.proxy(new AwsProxyRequestBuilder("/test", "GET").build(), new MockLambdaContext());
        } catch (Exception e) {
            assertNotNull(e);
            assertNotNull(e.getCause());
            assertTrue(e.getCause() instanceof RequestAbortedException);
            assertEquals(ExceptionContainerHandlerTest.NON_RUNTIME_MESSAGE, e.getCause().getMessage());
            return;
        }
        fail("Did not throw exception");
    }

    @Test
    void noException_returnsResponse() {
        throwException = false;
        LambdaContainerHandler.getContainerConfig().setDisableExceptionMapper(false);
        APIGatewayProxyResponseEvent resp = handler.proxy(new AwsProxyRequestBuilder("/test", "GET").build(), new MockLambdaContext());
        assertEquals(200, resp.getStatusCode());
        assertEquals("OK", resp.getBody());
    }

    public class ExceptionContainerHandlerTest extends LambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent, HttpServletRequest, AwsHttpServletResponse> {

        public static final String RUNTIME_MESSAGE = "test RuntimeException";
        public static final String NON_RUNTIME_MESSAGE = "test NonRuntimeException";

        protected ExceptionContainerHandlerTest(Class<APIGatewayProxyRequestEvent> requestClass, Class<APIGatewayProxyResponseEvent> responseClass, RequestReader<APIGatewayProxyRequestEvent, HttpServletRequest> requestReader, ResponseWriter<AwsHttpServletResponse, APIGatewayProxyResponseEvent> responseWriter, SecurityContextWriter<APIGatewayProxyRequestEvent> securityContextWriter, ExceptionHandler<APIGatewayProxyResponseEvent> exceptionHandler, InitializationWrapper init) {
            super(requestClass, responseClass, requestReader, responseWriter, securityContextWriter, exceptionHandler, init);
        }

        @Override
        protected AwsHttpServletResponse getContainerResponse(HttpServletRequest request, CountDownLatch latch) {
            return new AwsHttpServletResponse(request, latch);
        }

        @Override
        protected void handleRequest(HttpServletRequest containerRequest, AwsHttpServletResponse containerResponse, Context lambdaContext) throws Exception {
            if (throwException) {
                if (isRuntimeException) {
                    throw new RuntimeException(RUNTIME_MESSAGE);
                } else {
                    throw new RequestAbortedException(NON_RUNTIME_MESSAGE);
                }
            }
            containerResponse.setStatus(200);
            containerResponse.getWriter().print("OK");
            containerResponse.flushBuffer();
        }

        @Override
        public void initialize() throws ContainerInitializationException {

        }
    }
}
