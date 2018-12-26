package com.amazonaws.serverless.proxy.struts2;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

/**
 * The lambda handler to handle the requests.
 * <p>
 * <code>
 * com.amazonaws.serverless.proxy.struts2.Struts2LambdaHandler::handleRequest
 * </code>
 */
public class Struts2LambdaHandler implements RequestStreamHandler {

    private static final Logger log = LoggerFactory.getLogger(Struts2LambdaHandler.class);

    private final Struts2LambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = Struts2LambdaContainerHandler
            .getAwsProxyHandler();

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}
