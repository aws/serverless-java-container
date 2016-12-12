package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.Test;

import static org.junit.Assert.*;

public class AwsProxySecurityContextTest {
    private static final String COGNITO_IDENTITY_ID = "us-east-2:123123123123";
    private static final AwsProxyRequest REQUEST_NO_AUTH = new AwsProxyRequestBuilder("/hello", "GET").build();
    private static final AwsProxyRequest REQUEST_COGNITO_USER_POOL = new AwsProxyRequestBuilder("/hello", "GET")
            .cognitoUserPool(COGNITO_IDENTITY_ID).build();

    @Test
    public void localVars_constructor_nullValues() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, null);
        assertNull(context.event);
        assertNull(context.lambdaContext);
    }

    @Test
    public void localVars_constructor_ValidRequest() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_NO_AUTH);
        assertEquals(REQUEST_NO_AUTH, context.event);
        assertNull(context.lambdaContext);
    }

    @Test
    public void authScheme_getAuthenticationScheme_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertNotNull(context.getAuthenticationScheme());
        assertEquals("COGNITO_USER_POOL", context.getAuthenticationScheme());
    }

    @Test
    public void authScheme_getPrincipal_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertEquals("COGNITO_USER_POOL", context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }
}
