package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.ws.rs.core.SecurityContext;

import java.lang.reflect.Method;

import static org.junit.jupiter.api.Assertions.*;

public class AwsProxySecurityContextWriterTest {
    private AwsProxySecurityContextWriter writer;

    @BeforeEach
    public void setUp() {
        writer = new AwsProxySecurityContextWriter();
    }

    @Test
    void write_returnClass_securityContext()
            throws NoSuchMethodException {
        Method writeMethod = writer.getClass().getMethod("writeSecurityContext", AwsProxyRequest.class, Context.class);
        assertEquals(SecurityContext.class, writeMethod.getReturnType());
    }

    @Test
    void write_noAuth_emptySecurityContext() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test").build();
        SecurityContext context = writer.writeSecurityContext(request, null);

        assertNotNull(context);
        assertNull(context.getAuthenticationScheme());
        assertFalse(context.isSecure());
    }
}
