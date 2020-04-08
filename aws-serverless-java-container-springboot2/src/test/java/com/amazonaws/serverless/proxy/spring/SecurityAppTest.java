package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.securityapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.securityapp.SecurityConfig;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

public class SecurityAppTest {

    LambdaHandler handler = new LambdaHandler();
    MockLambdaContext lambdaContext = new MockLambdaContext();

    public SecurityAppTest() {
        System.setProperty("logging.level.root", "DEBUG");
    }

    @Test
    public void helloRequest_withAuth_respondsWithSingleMessage() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertEquals(401, resp.getStatusCode());
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.WWW_AUTHENTICATE));
        req = new AwsProxyRequestBuilder("/hello", "GET")
                .basicAuth(SecurityConfig.USERNAME, SecurityConfig.PASSWORD)
                .header(HttpHeaders.ACCEPT, MediaType.TEXT_PLAIN)
                .build();
        resp = handler.handleRequest(req, lambdaContext);
        assertEquals(200, resp.getStatusCode());
    }
}
