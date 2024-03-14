package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.junit.jupiter.api.Assertions.fail;

public class VPCLatticeV2RequestEventTest {
    public static final String BASE_V2_EVENT_AUTH_IAM = "{\n" +
            "  \"version\": \"2.0\",\n" +
            "  \"path\": \"/\",\n" +
            "  \"method\": \"GET\",\n" +
            "  \"headers\": {\n" +
            "    \"header-key\": [\"header-value\"]\n" +
            "  },\n" +
            "  \"queryStringParameters\": {\n" +
            "    \"key\": \"value\"\n" +
            "  },\n" +
            "  \"body\": \"request-body\",\n" +
            "  \"requestContext\": {\n" +
            "    \"serviceNetworkArn\": \"arn:aws:vpc-lattice:region:123456789012:servicenetwork/sn-0bf3f2882e9cc805a\",\n" +
            "    \"serviceArn\": \"arn:aws:vpc-lattice:region:123456789012:service/svc-0a40eebed65f8d69c\",\n" +
            "    \"targetGroupArn\": \"arn:aws:vpc-lattice:region:123456789012:targetgroup/tg-6d0ecf831eec9f09\",\n" +
            "    \"identity\": {\n" +
            "      \"sourceVpcArn\": \"arn:aws:ec2:region:123456789012:vpc/vpc-0b8276c84697e7339\",\n" +
            "      \"type\": \"AWS_IAM\",\n" +
            "      \"principal\": \"arn:aws:iam::123456789012:assumed-role/my-role/my-session\",\n" +
            "      \"sessionName\": \"i-0c7de02a688bde9f7\"\n" +
            "    },\n" +
            "    \"region\": \"region\",\n" +
            "    \"timeEpoch\": \"1690497599177430\"\n" +
            "  }\n" +
            "}";

    private static final String BASE_V2_EVENT_AUTH_NONE = "{\n" +
            "  \"version\": \"2.0\",\n" +
            "  \"path\": \"/\",\n" +
            "  \"method\": \"GET\",\n" +
            "  \"headers\": {\n" +
            "    \"header-key\": [\"header-value\"]\n" +
            "  },\n" +
            "  \"queryStringParameters\": {\n" +
            "    \"key\": \"value\"\n" +
            "  },\n" +
            "  \"body\": \"request-body\",\n" +
            "  \"requestContext\": {\n" +
            "    \"serviceNetworkArn\": \"arn:aws:vpc-lattice:region:123456789012:servicenetwork/sn-0bf3f2882e9cc805a\",\n" +
            "    \"serviceArn\": \"arn:aws:vpc-lattice:region:123456789012:service/svc-0a40eebed65f8d69c\",\n" +
            "    \"targetGroupArn\": \"arn:aws:vpc-lattice:region:123456789012:targetgroup/tg-6d0ecf831eec9f09\",\n" +
            "    \"identity\": {\n" +
            "      \"sourceVpcArn\": \"arn:aws:ec2:region:123456789012:vpc/vpc-0b8276c84697e7339\"\n" +
            "    },\n" +
            "    \"region\": \"region\",\n" +
            "    \"timeEpoch\": \"1690497599177430\"\n" +
            "  }\n" +
            "}";

    @Test
    void deserialize_fromJsonString_withIamAuth_populatesFieldsCorrectly() {
        try {
            VPCLatticeV2RequestEvent req = LambdaContainerHandler.getObjectMapper().readValue(BASE_V2_EVENT_AUTH_IAM, VPCLatticeV2RequestEvent.class);
            assertEquals("AWS_IAM", req.getRequestContext().getIdentity().getType());
            assertEquals("2.0", req.getVersion());
            assertEquals("/", req.getPath());
            assertEquals("GET", req.getMethod());
            assertEquals("request-body", req.getBody());
            assertNotNull(req.getHeaders());
            assertNotEquals(Boolean.TRUE, req.getIsBase64Encoded());
            assertNotNull(req.getQueryStringParameters());
            assertNotNull(req.getRequestContext());
            assertEquals("arn:aws:vpc-lattice:region:123456789012:servicenetwork/sn-0bf3f2882e9cc805a", req.getRequestContext().getServiceNetworkArn());
            assertEquals("arn:aws:vpc-lattice:region:123456789012:service/svc-0a40eebed65f8d69c", req.getRequestContext().getServiceArn());
            assertEquals("arn:aws:vpc-lattice:region:123456789012:targetgroup/tg-6d0ecf831eec9f09", req.getRequestContext().getTargetGroupArn());
            assertNotNull(req.getRequestContext().getIdentity());
            assertEquals("arn:aws:ec2:region:123456789012:vpc/vpc-0b8276c84697e7339", req.getRequestContext().getIdentity().getSourceVpcArn());
            assertEquals("arn:aws:iam::123456789012:assumed-role/my-role/my-session", req.getRequestContext().getIdentity().getPrincipal());
            assertEquals("i-0c7de02a688bde9f7", req.getRequestContext().getIdentity().getSessionName());
            assertNull(req.getRequestContext().getIdentity().getX509IssuerOu());
            assertNull(req.getRequestContext().getIdentity().getX509SanDns());
            assertNull(req.getRequestContext().getIdentity().getX509SanNameCn());
            assertNull(req.getRequestContext().getIdentity().getX509SanUri());
            assertNull(req.getRequestContext().getIdentity().getX509SubjectCn());
            assertEquals("1690497599177430", req.getRequestContext().getTimeEpoch());
            assertEquals("region", req.getRequestContext().getRegion());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }

    }
    @Test
    void deserialize_fromJsonString_populatesAuthTypeCorrectly() {
        try {
            VPCLatticeV2RequestEvent req = LambdaContainerHandler.getObjectMapper().readValue(BASE_V2_EVENT_AUTH_IAM, VPCLatticeV2RequestEvent.class);
            assertEquals("AWS_IAM", req.getRequestContext().getIdentity().getType());

            VPCLatticeV2RequestEvent req2 = LambdaContainerHandler.getObjectMapper().readValue(BASE_V2_EVENT_AUTH_NONE, VPCLatticeV2RequestEvent.class);
            assertNull(req2.getRequestContext().getIdentity().getType());
            assertEquals(RequestSource.VPC_LATTICE_V2, req.getRequestSource());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void requestEvent_requestContextBuilder_populatesCorrectly() {
        VPCLatticeV2RequestEvent.Identity identity = VPCLatticeV2RequestEvent.Identity.builder()
                .withPrincipal("arn:aws:iam::123456789012:assumed-role/my-role/my-session")
                .withSourceVpcArn("arn:aws:ec2:region:123456789012:vpc/vpc-0b8276c84697e7339")
                .withType("AWS_IAM")
                .withSessionName("i-0c7de02a688bde9f7")
                .build();

        VPCLatticeV2RequestEvent.RequestContext requestContext = new VPCLatticeV2RequestEvent.RequestContext.RequestContextBuilder()
                .withIdentity(identity)
                .withRegion("us-west-2")
                .withServiceArn("arn:aws:vpc-lattice:region:123456789012:service/svc-0a40eebed65f8d69c")
                .withServiceNetworkArn("arn:aws:vpc-lattice:region:123456789012:servicenetwork/sn-0bf3f2882e9cc805a")
                .withTargetGroupArn("arn:aws:vpc-lattice:region:123456789012:targetgroup/tg-6d0ecf831eec9f09")
                .withTimeEpoch("1690497599177430")
                .build();

        VPCLatticeV2RequestEvent event = new VPCLatticeV2RequestEvent.VPCLatticeV2RequestEventBuilder()
                .withRequestContext(requestContext)
                .build();

        assertEquals(requestContext, event.getRequestContext());
        assertEquals(identity, event.getRequestContext().getIdentity());
    }


}
