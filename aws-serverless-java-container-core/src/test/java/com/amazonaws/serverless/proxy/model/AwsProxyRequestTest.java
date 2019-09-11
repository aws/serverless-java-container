package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;

import java.io.IOException;

import static junit.framework.TestCase.assertTrue;
import static org.junit.Assert.*;

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
                                "    \"isBase64Encoded\": true\n" +
                                "}";

    @Test
    public void deserialize_multiValuedHeaders_caseInsensitive() throws IOException {
        AwsProxyRequest req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(true, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertNotNull(req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()));
        assertEquals(CUSTOM_HEADER_VALUE, req.getMultiValueHeaders().get(CUSTOM_HEADER_KEY_LOWER_CASE.toUpperCase()).get(0));
        assertTrue(req.isBase64Encoded());
    }

    @Test
    public void deserialize_base64Encoded_readsBoolCorrectly() throws IOException {
        AwsProxyRequest req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(true, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertTrue(req.isBase64Encoded());
        req = new AwsProxyRequestBuilder()
                .fromJsonString(getRequestJson(false, CUSTOM_HEADER_KEY_LOWER_CASE, CUSTOM_HEADER_VALUE)).build();
        assertFalse(req.isBase64Encoded());
    }

    @Test
    public void serialize_base64Encoded_fieldContainsIsPrefix() throws IOException {
        AwsProxyRequest req = new AwsProxyRequestBuilder()
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
}
