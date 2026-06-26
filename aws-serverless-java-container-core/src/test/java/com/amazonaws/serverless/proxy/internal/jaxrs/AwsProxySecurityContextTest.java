package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import java.security.Principal;

import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.ALB_ACESS_TOKEN_HEADER;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.ALB_IDENTITY_HEADER;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_AWS_IAM;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_COGNITO_POOL;
import static com.amazonaws.serverless.proxy.internal.jaxrs.AwsProxySecurityContext.AUTH_SCHEME_CUSTOM;
import static org.junit.jupiter.api.Assertions.*;

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
    private static final AwsProxyRequest ALB_REQUEST_MULTIPLE_HEADERS = new AwsProxyRequestBuilder("/hello", "GET")
            .alb()
            .header(ALB_ACESS_TOKEN_HEADER, "xxxxx")
            .header(ALB_IDENTITY_HEADER, "test-identity")
            .header(ALB_IDENTITY_HEADER, COGNITO_IDENTITY_ID)
            .build();

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
    void alb_noAuth_expectEmptyScheme() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, ALB_REQUEST_NO_AUTH);
        assertEquals(ALB_REQUEST_NO_AUTH, context.getEvent());
        assertNull(context.getLambdaContext());
        assertFalse(context.isSecure());
        assertNull(context.getAuthenticationScheme());
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
    void alb_cognitoAuth_expectCustomSchemeAndCorrectPrincipal() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, ALB_REQUEST_COGNITO_USER_POOL);
        assertTrue(context.isSecure());
        assertEquals(AUTH_SCHEME_CUSTOM, context.getAuthenticationScheme());
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

    @Test
    void alb_multipleIdentityHeaders_usesLastValue() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, ALB_REQUEST_MULTIPLE_HEADERS);
        assertEquals(COGNITO_IDENTITY_ID, context.getUserPrincipal().getName());
    }

    @Test
    void userPool_getClaims_retrieveCustomClaim() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_COGNITO_USER_POOL);
        Principal userPrincipal = context.getUserPrincipal();
        assertNotNull(userPrincipal.getName());
        assertEquals(COGNITO_IDENTITY_ID, userPrincipal.getName());

        assertTrue(userPrincipal instanceof AwsProxySecurityContext.CognitoUserPoolPrincipal);
        assertNotNull(((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().getClaim(CLAIM_KEY));
        assertEquals(CLAIM_VALUE, ((AwsProxySecurityContext.CognitoUserPoolPrincipal)userPrincipal).getClaims().getClaim(CLAIM_KEY));
    }

    @Test
    void constructor_withLambdaContext_returnsLambdaContext() {
        Context mockContext = Mockito.mock(Context.class);
        AwsProxySecurityContext context = new AwsProxySecurityContext(mockContext, REQUEST_NO_AUTH);
        assertEquals(mockContext, context.getLambdaContext());
    }

    @Test
    void iamAuth_getAuthenticationScheme_returnsAWSIAM() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().getIdentity().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        assertEquals(AUTH_SCHEME_AWS_IAM, context.getAuthenticationScheme());
        assertTrue(context.isSecure());
    }

    @Test
    void iamAuth_getUserPrincipal_returnsUserArn() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().getIdentity().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        request.getRequestContext().getIdentity().setUserArn("arn:aws:iam::123456789012:user/test");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        assertEquals(AUTH_SCHEME_AWS_IAM, context.getAuthenticationScheme());
        Principal principal = context.getUserPrincipal();
        assertEquals("arn:aws:iam::123456789012:user/test", principal.getName());
    }

    @Test
    void iamAuth_withCognitoIdentityId_returnsCognitoIdentityId() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().getIdentity().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        request.getRequestContext().getIdentity().setCognitoIdentityId("us-east-2:abc-123");
        request.getRequestContext().getIdentity().setUserArn("arn:aws:iam::123456789012:user/test");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        Principal principal = context.getUserPrincipal();
        assertEquals("us-east-2:abc-123", principal.getName());
    }

    @Test
    void customAuthorizer_noClaims_returnsCustomScheme() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().setAuthorizer(new com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext());
        request.getRequestContext().getAuthorizer().setPrincipalId("custom-user-123");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        assertEquals(AUTH_SCHEME_CUSTOM, context.getAuthenticationScheme());
        assertTrue(context.isSecure());
    }

    @Test
    void customAuthorizer_noClaims_getUserPrincipal_returnsPrincipalId() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().setAuthorizer(new com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext());
        request.getRequestContext().getAuthorizer().setPrincipalId("custom-user-123");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        Principal principal = context.getUserPrincipal();
        assertEquals("custom-user-123", principal.getName());
    }

    @Test
    void noAuth_getUserPrincipal_returnsNullName() {
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, REQUEST_NO_AUTH);
        assertNull(context.getAuthenticationScheme());
        assertFalse(context.isSecure());
        Principal principal = context.getUserPrincipal();
        assertNull(principal.getName());
    }

    @Test
    void iamAuth_isUserInRole_matchingRole_returnsTrue() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().getIdentity().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        request.getRequestContext().getIdentity().setUserArn("arn:aws:iam::123456789012:user/test");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        assertTrue(context.isUserInRole("arn:aws:iam::123456789012:user/test"));
    }

    @Test
    void iamAuth_isUserInRole_nonMatchingRole_returnsFalse() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/hello", "GET")
                .build();
        request.getRequestContext().getIdentity().setAccessKey("AKIAIOSFODNN7EXAMPLE");
        request.getRequestContext().getIdentity().setUserArn("arn:aws:iam::123456789012:user/test");
        AwsProxySecurityContext context = new AwsProxySecurityContext(null, request);
        assertFalse(context.isUserInRole("arn:aws:iam::123456789012:user/other"));
    }
}
