package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

public class AwsProxyRequestTest {
    private static final String CUSTOM_HEADER_KEY_LOWER_CASE = "custom-header";
    private static final String CUSTOM_HEADER_VALUE = "123456";
    public static final String REQUEST_JSON = "{\n" +
                                "    \"resource\": \"/api/{proxy+}\",\n" +
                                "    \"path\": \"/api/endpoint\",\n" +
                                "    \"httpMethod\": \"OPTIONS\",\n" +
                                "    \"headers\": {\n" +
                                "        \"Accept\": \"*/*\",\n" +
                                "        \"User-Agent\": \"PostmanRuntime/7.1.1\",\n" +
                                "        \"" + CUSTOM_HEADER_KEY_LOWER_CASE +"\":" + "\"" + CUSTOM_HEADER_VALUE + "\"\n" +
                                "    },\n" +
                                "    \"multiValueHeaders\": {\n" +
                                "        \"Accept\": [\n" +
                                "            \"*/*\"\n" +
                                "        ],\n" +
                                "        \"User-Agent\": [\n" +
                                "            \"PostmanRuntime/7.1.1\"\n" +
                                "        ],\n" +
                                "        \"" + CUSTOM_HEADER_KEY_LOWER_CASE + "\": [\n" +
                                "            \"" + CUSTOM_HEADER_VALUE + "\"\n" +
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
                                "    \"isBase64Encoded\": false\n" +
                                "}";

    @Test
    public void deserialize_multiValuedHeaders_caseInsensitive() throws IOException {
        AwsProxyRequest req = new AwsProxyRequestBuilder().fromJsonString(REQUEST_JSON).build();
        assertNotNull(req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()));
        assertEquals(CUSTOM_HEADER_VALUE, req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()).get(0));
    }
}
