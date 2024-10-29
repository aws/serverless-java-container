package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.amazonaws.services.lambda.runtime.Context;
import jakarta.ws.rs.core.SecurityContext;

import java.security.Principal;
import java.util.Objects;

/**
 * default implementation of the <code>SecurityContext</code> object. This class supports 1 VPC Lattice authentication type:
 * AWS_IAM.
 */
public class AwsVpcLatticeV2SecurityContext implements SecurityContext {

    static final String AUTH_SCHEME_AWS_IAM = "AWS_IAM";


    private final VPCLatticeV2RequestEvent event;

    public AwsVpcLatticeV2SecurityContext(Context lambdaContext, VPCLatticeV2RequestEvent event) {
        this.event = event;
    }

    //-------------------------------------------------------------
    // Implementation - SecurityContext
    //-------------------------------------------------------------
    @Override
    public Principal getUserPrincipal() {
        if (Objects.equals(getAuthenticationScheme(), AUTH_SCHEME_AWS_IAM)) {
            return () -> getEvent().getRequestContext().getIdentity().getPrincipal();
        }
        return null;
    }

    private VPCLatticeV2RequestEvent getEvent() {
        return event;
    }


    @Override
    public boolean isUserInRole(String role) {
        return role.equals(event.getRequestContext().getIdentity().getPrincipal());
    }

    @Override
    public boolean isSecure() {
        return getAuthenticationScheme() != null;
    }

    @Override
    public String getAuthenticationScheme() {
        if (Objects.equals(getEvent().getRequestContext().getIdentity().getType(), AUTH_SCHEME_AWS_IAM)) {
            return AUTH_SCHEME_AWS_IAM;
        } else {
            return null;
        }
    }
}
