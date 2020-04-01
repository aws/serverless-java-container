package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.internal.jaxrs.AwsHttpApiV2SecurityContext;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import javax.ws.rs.core.SecurityContext;

public class AwsHttpApiV2SecurityContextWriter implements SecurityContextWriter<HttpApiV2ProxyRequest> {
    @Override
    public SecurityContext writeSecurityContext(HttpApiV2ProxyRequest event, Context lambdaContext) {
        return new AwsHttpApiV2SecurityContext(lambdaContext, event);
    }
}
