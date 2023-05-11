package com.amazonaws.serverless.sample.jersey;


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


public class StreamLambdaHandler implements RequestStreamHandler {
    private static final ResourceConfig jerseyApplication = new ResourceConfig()
            // properties to speed up Jersey start time
            .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE,true)
            .property(ServerProperties.WADL_FEATURE_DISABLE,true)
            .property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE,true)
            .property(ServerProperties.BV_FEATURE_DISABLE,true)
            .property(ServerProperties.JSON_PROCESSING_FEATURE_DISABLE,true)
            .property(ServerProperties.MOXY_JSON_FEATURE_DISABLE,true)
                                                             .packages("com.amazonaws.serverless.sample.jersey")
                                                             .register(JacksonFeature.class);
    private static final JerseyLambdaContainerHandler<APIGatewayProxyRequestEvent, AwsProxyResponse> handler
            = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);

    public StreamLambdaHandler() {
        // we enable the timer for debugging. This SHOULD NOT be enabled in production.
        Timer.enable();
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
