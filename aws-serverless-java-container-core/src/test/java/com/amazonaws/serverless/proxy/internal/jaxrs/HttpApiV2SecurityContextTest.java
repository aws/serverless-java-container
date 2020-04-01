package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.AwsHttpApiV2SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import org.junit.Test;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import static org.junit.Assert.*;

public class HttpApiV2SecurityContextTest {
    private static final String JWT_SUB_VALUE = "1234567890";

    HttpApiV2ProxyRequest EMPTY_AUTH = new AwsProxyRequestBuilder("/", "GET").toHttpApiV2Request();
    HttpApiV2ProxyRequest BASIC_AUTH = new AwsProxyRequestBuilder("/", "GET")
            .authorizerPrincipal("test").toHttpApiV2Request();
    HttpApiV2ProxyRequest JWT_AUTH = new AwsProxyRequestBuilder("/", "GET")
            .authorizerPrincipal("test")
            .header(HttpHeaders.AUTHORIZATION, "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9.eyJzdWIiOiIxMjM0NTY3ODkwIiwibmFtZSI6IkpvaG4gRG9lIiwiaWF0IjoxNTE2MjM5MDIyfQ.SflKxwRJSMeKKF2QT4fwpMeJf36POk6yJV_adQssw5c")
            .toHttpApiV2Request();

    AwsHttpApiV2SecurityContextWriter contextWriter = new AwsHttpApiV2SecurityContextWriter();

    @Test
    public void getAuthenticationScheme_nullAuth_nullSchema() {
        SecurityContext ctx = contextWriter.writeSecurityContext(EMPTY_AUTH, null);
        assertNull(ctx.getAuthenticationScheme());
        assertNull(ctx.getUserPrincipal());
        assertFalse(ctx.isSecure());
    }

    @Test
    public void getAuthenticationScheme_jwtAuth_correctSchema() {
        SecurityContext ctx = contextWriter.writeSecurityContext(BASIC_AUTH, null);
        assertEquals(AwsHttpApiV2SecurityContext.AUTH_SCHEME_JWT, ctx.getAuthenticationScheme());
        assertTrue(ctx.isSecure());
        assertNull(ctx.getUserPrincipal());
    }

    @Test
    public void getPrincipal_parseJwt_returnsSub() {
        SecurityContext ctx = contextWriter.writeSecurityContext(JWT_AUTH, null);
        assertEquals(AwsHttpApiV2SecurityContext.AUTH_SCHEME_JWT, ctx.getAuthenticationScheme());
        assertTrue(ctx.isSecure());
        assertEquals(JWT_SUB_VALUE, ctx.getUserPrincipal().getName());
    }
}
