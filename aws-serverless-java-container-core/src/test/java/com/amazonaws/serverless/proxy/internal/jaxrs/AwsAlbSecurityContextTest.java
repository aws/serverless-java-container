package com.amazonaws.serverless.proxy.internal.jaxrs;

import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.*;
import static org.junit.jupiter.api.Assertions.*;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import org.junit.jupiter.api.Test;


public class AwsAlbSecurityContextTest {
    private static final ApplicationLoadBalancerRequestEvent ALB_REQUEST_NO_AUTH = new AwsProxyRequestBuilder("/hello", "GET").toAlbRequest();
    private static final String COGNITO_IDENTITY_ID = "us-east-2:123123123123";
    private static final ApplicationLoadBalancerRequestEvent ALB_REQUEST_COGNITO_USER_POOL = new AwsProxyRequestBuilder("/hello", "GET")
            .header(ALB_ACESS_TOKEN_HEADER, "xxxxx")
            .header(ALB_IDENTITY_HEADER, COGNITO_IDENTITY_ID)
            .toAlbRequest();


    @Test
    void alb_noAuth_expectEmptyScheme() {
        AwsAlbSecurityContext context = new AwsAlbSecurityContext(null, ALB_REQUEST_NO_AUTH);
        assertEquals(ALB_REQUEST_NO_AUTH, context.getEvent());
        assertNull(context.getLambdaContext());
        assertFalse(context.isSecure());
        assertNull(context.getAuthenticationScheme());
    }

    @Test
    void alb_cognitoAuth_expectCustomSchemeAndCorrectPrincipal() {
        AwsAlbSecurityContext context = new AwsAlbSecurityContext(null, ALB_REQUEST_COGNITO_USER_POOL);
        assertTrue(context.isSecure());
        assertEquals(AUTH_SCHEME_CUSTOM, context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

}
