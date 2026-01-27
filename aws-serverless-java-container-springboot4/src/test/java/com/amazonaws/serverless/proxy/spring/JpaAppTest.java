package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.jpaapp.LambdaHandler;
import com.amazonaws.serverless.proxy.spring.jpaapp.MessageController;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class JpaAppTest {

    LambdaHandler handler;
    MockLambdaContext lambdaContext = new MockLambdaContext();

    private String type;

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    public void initJpaAppTest(String reqType) {
        type = reqType;
        handler = new LambdaHandler(type);
    }

    @MethodSource("data")
    @ParameterizedTest
    void asyncRequest(String reqType) {
        initJpaAppTest(reqType);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/async", "POST")
                .json()
                .body("{\"name\":\"kong\"}");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertEquals("{\"name\":\"KONG\"}", resp.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void helloRequest_respondsWithSingleMessage(String reqType) {
        initJpaAppTest(reqType);
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/hello", "GET");
        AwsProxyResponse resp = handler.handleRequest(req, lambdaContext);
        assertEquals(MessageController.HELLO_MESSAGE, resp.getBody());
    }

}
