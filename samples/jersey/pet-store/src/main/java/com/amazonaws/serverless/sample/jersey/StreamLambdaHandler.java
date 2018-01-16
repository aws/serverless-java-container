package com.amazonaws.serverless.sample.jersey;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class StreamLambdaHandler implements RequestStreamHandler {
    private final ResourceConfig jerseyApplication = new ResourceConfig()
                                                             .packages("com.amazonaws.serverless.sample.jersey")
                                                             .register(JacksonFeature.class);
    private final JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler
            = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);

    public StreamLambdaHandler() {
        // we enable the timer for debugging. This SHOULD NOT be enabled in production.
        Timer.enable();
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {

        AwsProxyRequest request = LambdaContainerHandler.getObjectMapper().readValue(inputStream, AwsProxyRequest.class);

        AwsProxyResponse resp = handler.proxy(request, context);

        LambdaContainerHandler.getObjectMapper().writeValue(outputStream, resp);

        System.err.println(LambdaContainerHandler.getObjectMapper().writeValueAsString(Timer.getTimers()));

        // just in case it wasn't closed by the mapper
        outputStream.close();
    }
}
