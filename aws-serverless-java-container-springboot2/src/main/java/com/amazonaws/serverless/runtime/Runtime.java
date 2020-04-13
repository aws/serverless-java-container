package com.amazonaws.serverless.runtime;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class Runtime implements RequestStreamHandler {
    private static final int MAX_RETRIES = 3;

    private int maxRetries;
    private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    public Runtime(Class<?> initializer) throws ContainerInitializationException {
        this(initializer, MAX_RETRIES);
    }

    public Runtime(Class<?> initializer, int retries) throws ContainerInitializationException {
        maxRetries = retries;
        handler = SpringBootLambdaContainerHandler.getAwsProxyHandler(initializer);
    }

    public void start() {
        RuntimeClient client = new RuntimeClient();
        start(client);
    }

    public void start(RuntimeClient client) throws RuntimeException {
        while (true) {
            InvocationRequest req = getNextRequest(client);
            if (req == null) {
                throw new RuntimeException("Could not fetch next event after " + maxRetries + " attempts");
            }

            ByteArrayOutputStream os = new ByteArrayOutputStream();
            try {
                handleRequest(req.getEvent(), os, req.getContext());
            } catch (IOException e) {
                e.printStackTrace();
                postErrorResponse(client, req.getContext().getAwsRequestId(), e);
            }

            postResponse(client, req.getContext().getAwsRequestId(), os);
        }
    }

    private InvocationRequest getNextRequest(RuntimeClient client) {
        InvocationRequest req = null;
        for (int i = 0; i < maxRetries; i++) {
            try {
                req = client.getNextEvent();
                return req;
            } catch (RuntimeClientException e) {
                if (!e.isRetriable()) {
                    throw new RuntimeException("Failed to fetch next event", e);
                }
            }
        }
        return null;
    }

    private void postResponse(RuntimeClient client, String reqId, OutputStream os) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                client.postInvocationResponse(reqId, os);
                return;
            } catch (RuntimeClientException ex) {
                if (!ex.isRetriable()) {
                    throw new RuntimeException("Failed to post invocation response", ex);
                }
            }
        }
        throw new RuntimeException("Failed to post invocation response " + maxRetries + " times");
    }

    private void postErrorResponse(RuntimeClient client, String reqId, Throwable e) {
        for (int i = 0; i < maxRetries; i++) {
            try {
                client.postInvocationError(reqId, e);
                return;
            } catch (RuntimeClientException ex) {
                if (!ex.isRetriable()) {
                    throw new RuntimeException("Failed to post invocation error", ex);
                }
            }
        }
        throw new RuntimeException("Failed to post invocation error response " + maxRetries + " times");
    }

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context) throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
