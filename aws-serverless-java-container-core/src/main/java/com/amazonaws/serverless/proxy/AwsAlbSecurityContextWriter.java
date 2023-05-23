package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.internal.jaxrs.AwsAlbSecurityContext;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import jakarta.ws.rs.core.SecurityContext;

public class AwsAlbSecurityContextWriter implements SecurityContextWriter<ApplicationLoadBalancerRequestEvent> {

    private AwsAlbSecurityContext currentContext;

    @Override
    public SecurityContext writeSecurityContext(ApplicationLoadBalancerRequestEvent event, Context lambdaContext) {
        currentContext = new AwsAlbSecurityContext(lambdaContext, event);
        return currentContext;
    }

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public AwsAlbSecurityContext getCurrentContext() {
        return currentContext;
    }
}
