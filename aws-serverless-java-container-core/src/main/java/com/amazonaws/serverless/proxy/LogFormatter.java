package com.amazonaws.serverless.proxy;


import javax.ws.rs.core.SecurityContext;


/**
 * Implementations of the log formatter interface are used by {@link com.amazonaws.serverless.proxy.internal.LambdaContainerHandler} class to log each request
 * processed in the container. You can set the log formatter using the {@link com.amazonaws.serverless.proxy.internal.LambdaContainerHandler#setLogFormatter(LogFormatter)}
 * method. The servlet implementation of the container ({@link com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler} includes a
 * default log formatter that produces Apache combined logs. {@link com.amazonaws.serverless.proxy.internal.servlet.ApacheCombinedServletLogFormatter}.
 * @param <ContainerRequestType> The request type used by the underlying framework
 * @param <ContainerResponseType> The response type produced by the underlying framework
 */
public interface LogFormatter<ContainerRequestType, ContainerResponseType> {
    /**
     * The format method is called by the container handler to produce the log line that should be written to the logs.
     * @param req The incoming request
     * @param res The completed response
     * @param ctx The security context produced based on the request
     * @return The log line
     */
    String format(ContainerRequestType req, ContainerResponseType res, SecurityContext ctx);
}
