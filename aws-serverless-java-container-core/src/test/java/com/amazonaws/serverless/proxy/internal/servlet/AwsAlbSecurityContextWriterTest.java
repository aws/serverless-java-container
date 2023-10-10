package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import jakarta.ws.rs.core.SecurityContext;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.assertFalse;

public class AwsAlbSecurityContextWriterTest {
    private AwsAlbSecurityContextWriter writer;

    @BeforeEach
    public void setUp() {
        writer = new AwsAlbSecurityContextWriter();
    }

    @Test
    void write_returnClass_securityContext()
            throws NoSuchMethodException {
        Method writeMethod = writer.getClass().getMethod("writeSecurityContext", ApplicationLoadBalancerRequestEvent.class, Context.class);
        assertEquals(SecurityContext.class, writeMethod.getReturnType());
    }

    @Test
    void write_noAuth_emptySecurityContext() {
        ApplicationLoadBalancerRequestEvent request = new AwsProxyRequestBuilder("/test").toAlbRequest();
        SecurityContext context = writer.writeSecurityContext(request, null);

        assertNotNull(context);
        assertNull(context.getAuthenticationScheme());
        assertFalse(context.isSecure());
    }
}
