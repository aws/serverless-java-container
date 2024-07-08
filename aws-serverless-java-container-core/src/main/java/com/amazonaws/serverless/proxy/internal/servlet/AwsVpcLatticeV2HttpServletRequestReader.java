package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.amazonaws.services.lambda.runtime.Context;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.SecurityContext;

public class AwsVpcLatticeV2HttpServletRequestReader extends RequestReader<VPCLatticeV2RequestEvent, HttpServletRequest> {

    static final String INVALID_REQUEST_ERROR = "The incoming event is not a valid request from Amazon API Gateway or an Application Load Balancer";
    @Override
    public AwsVpcLatticeV2HttpServletRequest readRequest(VPCLatticeV2RequestEvent request, SecurityContext securityContext, Context lambdaContext, ContainerConfig config) throws InvalidRequestEventException {
        if ( request.getMethod() == null || request.getMethod().isEmpty() || request.getRequestContext() == null) {
            throw new InvalidRequestEventException(INVALID_REQUEST_ERROR);
        }

        // clean out the request path based on the container config
        request.setPath(stripBasePath(request.getPath(), config));

        AwsVpcLatticeV2HttpServletRequest servletRequest = new AwsVpcLatticeV2HttpServletRequest(request, lambdaContext, securityContext, config);
        servletRequest.setAttribute(VPC_LATTICE_V2_CONTEXT_PROPERTY, request.getRequestContext());
        servletRequest.setAttribute(VPC_LATTICE_V2_EVENT_PROPERTY, request);
        servletRequest.setAttribute(LAMBDA_CONTEXT_PROPERTY, lambdaContext);
        servletRequest.setAttribute(JAX_SECURITY_CONTEXT_PROPERTY, securityContext);

        return servletRequest;
    }

    @Override
    protected Class<? extends VPCLatticeV2RequestEvent> getRequestClass() {
        return VPCLatticeV2RequestEvent.class;
    }
}
