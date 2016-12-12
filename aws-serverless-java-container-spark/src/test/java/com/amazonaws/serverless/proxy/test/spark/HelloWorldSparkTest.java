package com.amazonaws.serverless.proxy.test.spark;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spark.SparkLambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import junit.framework.TestCase;

import static spark.Spark.get;

public class HelloWorldSparkTest extends TestCase {
    private ObjectMapper objectMapper = new ObjectMapper();

    public void testSparkStart() {
        try {
            SparkLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler
                    = SparkLambdaContainerHandler.getAwsProxyHandler();

            get("/hello", (req, res) -> {
                res.status(200);
                res.body("Hello World");
                res.header("X-Custom-Header", "My Header Value");
                return "Hello World";
            });

            AwsProxyRequest req = new AwsProxyRequestBuilder().method("GET").path("/hello").build();
            AwsProxyResponse response = handler.proxy(req, new MockLambdaContext());
            System.out.println("Response: " + objectMapper.writeValueAsString(response));
        } catch (JsonProcessingException | ContainerInitializationException e) {
            e.printStackTrace();
        }
    }
}
