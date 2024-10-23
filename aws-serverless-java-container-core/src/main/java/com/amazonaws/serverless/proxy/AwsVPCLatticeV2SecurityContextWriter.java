package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.internal.jaxrs.AwsVpcLatticeV2SecurityContext;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.amazonaws.services.lambda.runtime.Context;
import jakarta.ws.rs.core.SecurityContext;

public class AwsVPCLatticeV2SecurityContextWriter implements SecurityContextWriter<VPCLatticeV2RequestEvent>{
    @Override
    public SecurityContext writeSecurityContext(VPCLatticeV2RequestEvent event, Context lambdaContext) {
        return new AwsVpcLatticeV2SecurityContext(lambdaContext, event);
    }
}
