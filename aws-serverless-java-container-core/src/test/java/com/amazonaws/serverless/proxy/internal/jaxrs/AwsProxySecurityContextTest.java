package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.events.apigateway.APIGatewayProxyRequestEvent;
import org.junit.jupiter.api.Test;

import java.security.Principal;

import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_COGNITO_POOL;
import static org.junit.jupiter.api.Assertions.*;

public class AwsProxySecurityContextTest {
    private static final String CLAIM_KEY = "custom:claim";
    private static final String CLAIM_VALUE = "customClaimant";
    private static final String COGNITO_IDENTITY_ID = "us-east-2:123123123123";
    private static final APIGatewayProxyRequestEvent REQUEST_NO_AUTH = new AwsProxyRequestBuilder("/hello", "GET").build();
    private static final APIGatewayProxyRequestEvent REQUEST_COGNITO_USER_POOL = new AwsProxyRequestBuilder("/hello", "GET")
            .cognitoUserPool(COGNITO_IDENTITY_ID).claim(CLAIM_KEY, CLAIM_VALUE).build();

    @Test
    void localVars_constructor_nullValues() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, null);
        assertNull(context.getEvent());
        assertNull(context.getLambdaContext());
    }

    @Test
    void localVars_constructor_ValidRequest() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_NO_AUTH);
        assertEquals(REQUEST_NO_AUTH, context.getEvent());
        assertNull(context.getLambdaContext());
    }

    @Test
    void authScheme_getAuthenticationScheme_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertNotNull(context.getAuthenticationScheme());
        assertEquals(AUTH_SCHEME_COGNITO_POOL, context.getAuthenticationScheme());
    }

    @Test
    void authScheme_getPrincipal_userPool() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        assertEquals(AUTH_SCHEME_COGNITO_POOL, context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

    @Test
    void userPool_getClaims_retrieveCustomClaim() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        Principal userPrincipal = context.getUserPrincipal();
        assertNotNull(userPrincipal.getName());
        assertEquals(COGNITO_IDENTITY_ID, userPrincipal.getName());

        assertTrue(userPrincipal instanceof AwsProxySecurityContext.CognitoUserPoolPrincipal);
        assertNotNull(((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().get(CLAIM_KEY));
        assertEquals(CLAIM_VALUE, ((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().get(CLAIM_KEY));
    }
}
