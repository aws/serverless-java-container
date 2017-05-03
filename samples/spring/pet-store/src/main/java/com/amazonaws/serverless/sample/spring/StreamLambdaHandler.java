package com.amazonaws.serverless.sample.spring;


import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import com.fasterxml.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;


/**
 * Created by bulianis on 5/2/17.
 */
public class StreamLambdaHandler implements RequestStreamHandler {
    private SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    private static ObjectMapper mapper = new ObjectMapper();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        if (handler == null) {
            try {
                handler = SpringLambdaContainerHandler.getAwsProxyHandler(PetStoreSpringAppConfig.class);
            } catch (ContainerInitializationException e) {
                e.printStackTrace();
                outputStream.close();
            }
        }

        AwsProxyRequest request = mapper.readValue(inputStream, AwsProxyRequest.class);

        AwsProxyResponse resp = handler.proxy(request, context);

        mapper.writeValue(outputStream, resp);
        // just in case it wasn't closed by the mapper
        outputStream.close();
    }
}
