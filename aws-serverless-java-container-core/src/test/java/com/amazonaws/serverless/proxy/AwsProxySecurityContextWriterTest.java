package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Before;
import org.junit.Test;

import javax.ws.rs.core.SecurityContext;

import java.lang.reflect.Method;

import static org.junit.Assert.*;

public class AwsProxySecurityContextWriterTest {
    private AwsProxySecurityContextWriter writer;

    @Before
    public void setUp() {
        writer = new AwsProxySecurityContextWriter();
    }

    @Test
    public void write_returnClass_securityContext()
            throws NoSuchMethodException {
        Method writeMethod = writer.getClass().getMethod("writeSecurityContext", AwsProxyRequest.class, Context.class);
        assertEquals(SecurityContext.class, writeMethod.getReturnType());
    }

    @Test
    public void write_noAuth_emptySecurityContext() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/test").build();
        SecurityContext context = writer.writeSecurityContext(request, null);

        assertNotNull(context);
        assertNull(context.getAuthenticationScheme());
        assertFalse(context.isSecure());
    }
}
