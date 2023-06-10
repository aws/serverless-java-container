package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;

import static org.junit.jupiter.api.Assertions.*;

public class AwsHttpApiV2HttpServletResponseWriterTest {

    static AwsHttpApiV2HttpServletRequestReader requestReader = new AwsHttpApiV2HttpServletRequestReader();
    @Test
    void writeResponse_returnsValidResponse() throws InvalidRequestEventException, InvalidResponseObjectException {
        AwsHttpApiV2HttpServletResponseWriter responseWriter = new AwsHttpApiV2HttpServletResponseWriter(true);
        APIGatewayV2HTTPEvent v2Request = new AwsProxyRequestBuilder("/hello", "GET").toHttpApiV2Request();
        HttpServletRequest servletRequest = requestReader.readRequest(v2Request, null, new MockLambdaContext(), ContainerConfig.defaultConfig());
        AwsHttpServletResponse response = new AwsHttpServletResponse(servletRequest, new CountDownLatch(1));
        response.setAwsResponseBodyString("Random string");

        APIGatewayV2HTTPResponse res3 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertTrue(res3.getHeaders().isEmpty());

        response.setContentType("application/octet-stream");
        APIGatewayV2HTTPResponse res2 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertTrue(res2.getIsBase64Encoded());

        response.setHeader("Connection", "Keep-Alive");
        response.setContentType("application/octet-stream;");
        response.setStatus(200);
        APIGatewayV2HTTPResponse res1 = responseWriter.writeResponse(response, new MockLambdaContext());
        assertNotNull(res1);
        assertEquals(200, res1.getStatusCode());
    }
}
