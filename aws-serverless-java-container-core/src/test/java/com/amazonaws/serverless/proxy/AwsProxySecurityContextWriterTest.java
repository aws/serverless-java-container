package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
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
        Method writeMethod = writer.getClass().getMethod("writeSecurityContext", APIGatewayProxyRequestEvent.class, Context.class);
        assertEquals(SecurityContext.class, writeMethod.getReturnType());
    }

    @Test
    void write_noAuth_emptySecurityContext() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/test").build();
        SecurityContext context = writer.writeSecurityContext(request, null);

        assertNotNull(context);
        assertNull(context.getAuthenticationScheme());
        assertFalse(context.isSecure());
    }
}
