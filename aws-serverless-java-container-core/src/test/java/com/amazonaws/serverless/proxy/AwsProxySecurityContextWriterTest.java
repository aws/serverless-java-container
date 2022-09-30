package com.amazonaws.serverless.proxy;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import jakarta.ws.rs.core.SecurityContext;
import java.lang.reflect.Method;
import org.junit.Before;
import org.junit.Test;

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
