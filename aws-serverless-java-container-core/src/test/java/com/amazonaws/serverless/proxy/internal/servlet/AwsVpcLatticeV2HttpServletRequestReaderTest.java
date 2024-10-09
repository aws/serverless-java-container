package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AwsVpcLatticeV2HttpServletRequestReaderTest {
    private AwsVpcLatticeV2HttpServletRequestReader reader = new AwsVpcLatticeV2HttpServletRequestReader();

    @Test
    void reflection_getRequestClass_returnsCorrectType() {
        assertSame(VPCLatticeV2RequestEvent.class, reader.getRequestClass());
    }

    @Test
    void baseRequest_read_populatesSuccessfully() {
        VPCLatticeV2RequestEvent req = new AwsProxyRequestBuilder("/hello", "GET")
                .queryString("param1", "value1")
                .header("custom", "value")
                .toVPCLatticeV2Request();
        AwsVpcLatticeV2HttpServletRequestReader reader = new AwsVpcLatticeV2HttpServletRequestReader();
        try {
            HttpServletRequest servletRequest = reader.readRequest(req, null, null, LambdaContainerHandler.getContainerConfig());
            assertEquals("/hello", servletRequest.getPathInfo());
            assertEquals("value1", servletRequest.getParameter("param1"));
            assertEquals("value", servletRequest.getHeader("CUSTOM"));
            assertNotNull(servletRequest.getAttribute(AwsVpcLatticeV2HttpServletRequestReader.VPC_LATTICE_V2_CONTEXT_PROPERTY));
        } catch (InvalidRequestEventException e) {
            e.printStackTrace();
            fail("Could not read request");
        }
    }
}
