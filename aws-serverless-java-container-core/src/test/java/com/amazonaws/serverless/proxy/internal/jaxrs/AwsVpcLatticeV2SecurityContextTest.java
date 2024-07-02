package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.AwsVPCLatticeV2SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.Test;

import static com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEventTest.BASE_V2_EVENT_AUTH_IAM;
import static org.junit.jupiter.api.Assertions.*;

public class AwsVpcLatticeV2SecurityContextTest {


    AwsVPCLatticeV2SecurityContextWriter contextWriter = new AwsVPCLatticeV2SecurityContextWriter();
    VPCLatticeV2RequestEvent NONE_AUTH = new AwsProxyRequestBuilder("/", "GET").toVPCLatticeV2Request();



    private VPCLatticeV2RequestEvent getAuthenticatedEvent() {
        try {
            return LambdaContainerHandler.getObjectMapper().readValue(BASE_V2_EVENT_AUTH_IAM, VPCLatticeV2RequestEvent.class);
        } catch (JsonProcessingException e) {
            throw new RuntimeException(e);
        }
    }



    @Test
    void getAuthenticationScheme_noAuth_nullAuthType() {
        SecurityContext ctx = contextWriter.writeSecurityContext(NONE_AUTH, null);
        assertNull(ctx.getAuthenticationScheme());
        assertNull(ctx.getUserPrincipal());
        assertFalse(ctx.isSecure());
    }

    @Test
    void getAuthenticationScheme_iamAuth_AwsIamAuthType() {
        VPCLatticeV2RequestEvent req = getAuthenticatedEvent();
        SecurityContext ctx = contextWriter.writeSecurityContext(req, null);
        assertNotNull(ctx.getAuthenticationScheme());
        assertEquals(ctx.getUserPrincipal().getName(), "arn:aws:iam::123456789012:assumed-role/my-role/my-session");
        assertTrue(ctx.isSecure());
    }
}
