package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.Test;

import java.security.Principal;

import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.ALB_ACESS_TOKEN_HEADER;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.ALB_IDENTITY_HEADER;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_COGNITO_POOL;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_CUSTOM;
import static org.junit.Assert.*;

public class AwsProxySecurityContextTest {
    private static final String CLAIM_KEY = "custom:claim";
    private static final String CLAIM_VALUE = "customClaimant";
    private static final String COGNITO_IDENTITY_ID = "us-east-2:123123123123";
    private static final AwsProxyRequest REQUEST_NO_AUTH = new AwsProxyRequestBuilder("/hello", "GET").build();
    private static final AwsProxyRequest ALB_REQUEST_NO_AUTH = new AwsProxyRequestBuilder("/hello", "GET").alb().build();
    private static final AwsProxyRequest REQUEST_COGNITO_USER_POOL = new AwsProxyRequestBuilder("/hello", "GET")
            .cognitoUserPool(COGNITO_IDENTITY_ID).claim(CLAIM_KEY, CLAIM_VALUE).build();
    private static final AwsProxyRequest ALB_REQUEST_COGNITO_USER_POOL = new AwsProxyRequestBuilder("/hello", "GET")
            .alb()
            .header(ALB_ACESS_TOKEN_HEADER, "xxxxx")
            .header(ALB_IDENTITY_HEADER, COGNITO_IDENTITY_ID)
            .build();

    @Test
    public void localVars_constructor_nullValues() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, null);
        assertNull(context.getEvent());
        assertNull(context.getLambdaContext());
    }

    @Test
    public void localVars_constructor_ValidRequest() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_NO_AUTH);
        assertEquals(REQUEST_NO_AUTH, context.getEvent());
        assertNull(context.getLambdaContext());
    }

    @Test
    public void alb_noAuth_expectEmptyScheme() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, ALB_REQUEST_NO_AUTH);
        assertEquals(ALB_REQUEST_NO_AUTH, context.getEvent());
        assertNull(context.getLambdaContext());
        assertFalse(context.isSecure());
        assertNull(context.getAuthenticationScheme());
    }

    @Test
    public void authScheme_getAuthenticationScheme_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertNotNull(context.getAuthenticationScheme());
        assertEquals(AUTH_SCHEME_COGNITO_POOL, context.getAuthenticationScheme());
    }

    @Test
    public void authScheme_getPrincipal_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertEquals(AUTH_SCHEME_COGNITO_POOL, context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

    @Test
    public void alb_cognitoAuth_expectCustomSchemeAndCorrectPrincipal() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, ALB_REQUEST_COGNITO_USER_POOL);
        assertTrue(context.isSecure());
        assertEquals(AUTH_SCHEME_CUSTOM, context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

    @Test
    public void userPool_getClaims_retrieveCustomClaim() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        Principal userPrincipal = context.getUserPrincipal();
        assertNotNull(userPrincipal.getName());
        assertEquals(COGNITO_IDENTITY_ID, userPrincipal.getName());

        assertTrue(userPrincipal instanceof AwsProxySecurityContext.CognitoUserPoolPrincipal);
        assertNotNull(((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().getClaim(CLAIM_KEY));
        assertEquals(CLAIM_VALUE, ((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().getClaim(CLAIM_KEY));
    }
}
