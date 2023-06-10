package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Map;

public class AwsAlbSecurityContext implements SecurityContext {

    //-------------------------------------------------------------
    // Constants - Package
    //-------------------------------------------------------------

    static final String AUTH_SCHEME_CUSTOM = "CUSTOM_AUTHORIZER";
    static final String AUTH_SCHEME_AWS_IAM = "AWS_IAM";

    static final String ALB_ACCESS_TOKEN_HEADER = "x-amzn-oidc-accesstoken";
    static final String ALB_IDENTITY_HEADER = "x-amzn-oidc-identity";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Context lambdaContext;
    private ApplicationLoadBalancerRequestEvent event;


    public Context getLambdaContext() {
        return lambdaContext;
    }


    public ApplicationLoadBalancerRequestEvent getEvent() {
        return event;
    }

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsAlbSecurityContext(final Context lambdaContext, final ApplicationLoadBalancerRequestEvent event) {
        this.lambdaContext = lambdaContext;
        this.event = event;
    }

    //-------------------------------------------------------------
    // Implementation - SecurityContext
    //-------------------------------------------------------------
    @Override
    public Principal getUserPrincipal() {
        if (getAuthenticationScheme() == null) {
            return () -> null;
        }

        if (getAuthenticationScheme().equals(AUTH_SCHEME_CUSTOM) || getAuthenticationScheme().equals(AUTH_SCHEME_AWS_IAM)) {
            return () -> {
                if (getAuthenticationScheme().equals(AUTH_SCHEME_CUSTOM) && event.getMultiValueHeaders().get(ALB_IDENTITY_HEADER) != null) {
                    return event.getMultiValueHeaders().get(ALB_IDENTITY_HEADER).get(0);
                }

                // return null if we couldn't find a valid scheme
                return null;
            };
        }

        throw new RuntimeException("Cannot recognize authorization scheme in event");
    }

    @Override
    public boolean isUserInRole(String role) {
        return false;
    }

    @Override
    public boolean isSecure() {
        return getAuthenticationScheme() != null;
    }

    @Override
    public String getAuthenticationScheme() {
        if (event.getMultiValueHeaders().containsKey(ALB_ACCESS_TOKEN_HEADER)) {
            return AUTH_SCHEME_CUSTOM;
        }
        return null;
    }
}
