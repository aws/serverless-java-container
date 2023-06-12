package com.amazonaws.serverless.proxy.model;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.util.Arrays;

import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;

public class AwsProxyRequestTest {
    private static final String CUSTOM_HEADER_KEY_LOWER_CASE = "custom-header";
    private static final String CUSTOM_HEADER_VALUE = "123456";

    @Test
    void deserialize_multiValuedHeaders_caseInsensitive() throws IOException {
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(true, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertNotNull(req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()));
        assertEquals(CUSTOM_HEADER_VALUE, req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()).get(0));
        assertTrue(req.getIsBase64Encoded());
    }

    @Test
    void deserialize_base64Encoded_readsBoolCorrectly() throws IOException {
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(true, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertTrue(req.getIsBase64Encoded());
        req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(false, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertFalse(req.getIsBase64Encoded());
    }

    @Test
    void serialize_base64Encoded_fieldContainsIsPrefix() throws IOException {
        APIGatewayProxyRequestEvent req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(true, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        ObjectMapper mapper = new ObjectMapper();
        String serializedRequest = mapper.writeValueAsString(req);

        assertTrue(serializedRequest.contains("\"isBase64Encoded\":true"));
    }

    private String getRequestJson(boolean base64Encoded, String headerKey, String headerValue) {
        return "{\n" +
                "    \"resource\": \"/api/{proxy+}\",\n" +
                "    \"path\": \"/api/endpoint\",\n" +
                "    \"httpMethod\": \"OPTIONS\",\n" +
                "    \"headers\": {\n" +
                "        \"Accept\": \"*/*\",\n" +
                "        \"User-Agent\": \"PostmanRuntime/7.1.1\",\n" +
                "        \"" + headerKey +"\":" + "\"" + headerValue + "\"\n" +
                "    },\n" +
                "    \"multiValueHeaders\": {\n" +
                "        \"Accept\": [\n" +
                "            \"*/*\"\n" +
                "        ],\n" +
                "        \"User-Agent\": [\n" +
                "            \"PostmanRuntime/7.1.1\"\n" +
                "        ],\n" +
                "        \"" + headerKey + "\": [\n" +
                "            \"" + headerValue + "\"\n" +
                "        ]\n" +
                "    },\n" +
                "    \"queryStringParameters\": null,\n" +
                "    \"multiValueQueryStringParameters\": null,\n" +
                "    \"pathParameters\": {\n" +
                "        \"proxy\": \"endpoint\"\n" +
                "    },\n" +
                "    \"stageVariables\": null,\n" +
                "    \"requestContext\": {\n" +
                "        \"resourceId\": null,\n" +
                "        \"resourcePath\": \"/api/{proxy+}\",\n" +
                "        \"httpMethod\": \"OPTIONS\",\n" +
                "        \"extendedRequestId\": null,\n" +
                "        \"requestTime\": \"15/Dec/2018:20:37:47 +0000\",\n" +
                "        \"path\": \"/api/endpoint\",\n" +
                "        \"accountId\": null,\n" +
                "        \"protocol\": \"HTTP/1.1\",\n" +
                "        \"stage\": \"stage_name\",\n" +
                "        \"domainPrefix\": null,\n" +
                "        \"requestTimeEpoch\": 1544906267828,\n" +
                "        \"requestId\": null,\n" +
                "        \"identity\": {\n" +
                "            \"cognitoIdentityPoolId\": null,\n" +
                "            \"accountId\": null,\n" +
                "            \"cognitoIdentityId\": null,\n" +
                "            \"caller\": null,\n" +
                "            \"sourceIp\": \"54.240.196.171\",\n" +
                "            \"accessKey\": null,\n" +
                "            \"cognitoAuthenticationType\": null,\n" +
                "            \"cognitoAuthenticationProvider\": null,\n" +
                "            \"userArn\": null,\n" +
                "            \"userAgent\": \"PostmanRuntime/7.1.1\",\n" +
                "            \"user\": null\n" +
                "        },\n" +
                "        \"domainName\": \"https://apiId.execute-api.eu-central-1.amazonaws.com/\",\n" +
                "        \"apiId\": \"apiId\"\n" +
                "    },\n" +
                "    \"body\": null,\n" +
                "    \"isBase64Encoded\": " + (base64Encoded?"true":"false") + "\n" +
                "}";
    }

    @Test
    @Disabled
    void deserialize_singleValuedHeaders() throws IOException {
        ApplicationLoadBalancerRequestEvent req =
                new AwsProxyRequestBuilder().fromJsonString(getSingleValueRequestJson()).toAlbRequest();

        assertThat(req.getHeaders().get("accept"), is("*"));
    }

    /**
     * Captured from a live request to an ALB with a Lambda integration with 
     * lambda.multi_value_headers.enabled=false.
     */
    private String getSingleValueRequestJson() {
        return "{\n" + "        \"requestContext\": {\n" + "            \"elb\": {\n"
            + "                \"targetGroupArn\": \"arn:aws:elasticloadbalancing:us-east-2:123456789012:targetgroup/prod-example-function/e77803ebb6d2c24\"\n"
            + "            }\n" + "        },\n" + "        \"httpMethod\": \"PUT\",\n"
            + "        \"path\": \"/path/to/resource\",\n" + "        \"queryStringParameters\": {},\n"
            + "        \"headers\": {\n" + "            \"accept\": \"*\",\n"
            + "            \"content-length\": \"17\",\n"
            + "            \"content-type\": \"application/json\",\n"
            + "            \"host\": \"stackoverflow.name\",\n"
            + "            \"user-agent\": \"curl/7.77.0\",\n"
            + "            \"x-amzn-trace-id\": \"Root=1-62e22402-3a5f246225e45edd7735c182\",\n"
            + "            \"x-forwarded-for\": \"24.14.13.186\",\n"
            + "            \"x-forwarded-port\": \"443\",\n"
            + "            \"x-forwarded-proto\": \"https\",\n"
            + "            \"x-jersey-tracing-accept\": \"true\"\n" + "        },\n"
            + "        \"body\": \"{\\\"alpha\\\":\\\"bravo\\\"}\",\n"
            + "        \"isBase64Encoded\": false\n" + "}      \n";
    }
}
