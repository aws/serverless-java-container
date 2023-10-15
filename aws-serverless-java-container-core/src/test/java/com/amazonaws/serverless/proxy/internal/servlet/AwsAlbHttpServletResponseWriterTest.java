package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.AwsProxyResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AwsAlbHttpServletResponseWriterTest {
    private AwsAlbHttpServletRequestReader reader = new AwsAlbHttpServletRequestReader();
    static AwsAlbHttpServletRequestReader requestReader = new AwsAlbHttpServletRequestReader();

    @Test
    void writeResponse_returnsValidResponse() throws InvalidRequestEventException, InvalidResponseObjectException {
        AwsAlbHttpServletResponseWriter responseWriter = new AwsAlbHttpServletResponseWriter(true);
        ApplicationLoadBalancerRequestEvent albRequest = new AwsProxyRequestBuilder("/hello", "GET").toAlbRequest();
        HttpServletRequest servletRequest = requestReader.readRequest(albRequest, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        AwsHttpServletResponse response = new AwsHttpServletResponse(servletRequest, new CountDownLatch(1));
        response.setAwsResponseBodyString("Random string");

        AwsProxyResponseEvent res3 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertTrue(res3.getHeaders().isEmpty());

        response.setContentType("application/octet-stream");
        AwsProxyResponseEvent res2 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertTrue(res2.getIsBase64Encoded());

        response.setHeader("Connection", "Keep-Alive");
        response.setContentType("application/octet-stream;");
        response.setStatus(200);
        AwsProxyResponseEvent res1 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertNotNull(res1);
        assertEquals(200, res1.getStatusCode());
    }

}
