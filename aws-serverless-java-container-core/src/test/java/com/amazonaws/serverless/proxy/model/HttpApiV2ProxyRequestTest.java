package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.fasterxml.jackson.core.JsonProcessingException;
import org.junit.Test;

import java.util.ArrayList;

import static org.junit.Assert.*;

public class HttpApiV2ProxyRequestTest {

    private static final String BASE_PROXY_REQUEST = "{\n" +
            "      \"version\": \"2.0\",\n" +
            "      \"routeKey\": \"$default\",\n" +
            "      \"rawPath\": \"/my/path\",\n" +
            "      \"rawQueryString\": \"parameter1=value1&parameter1=value2&parameter2=value\",\n" +
            "      \"cookies\": [ \"cookie1\", \"cookie2\" ],\n" +
            "      \"headers\": {\n" +
            "        \"Header1\": \"value1\",\n" +
            "        \"Header2\": \"value2\"\n" +
            "      },\n" +
            "      \"queryStringParameters\": { \"parameter1\": \"value1,value2\", \"parameter2\": \"value\" },\n" +
            "      \"requestContext\": {\n" +
            "        \"accountId\": \"123456789012\",\n" +
            "        \"apiId\": \"api-id\",\n" +
            "        \"authorizer\": { \"jwt\": {\n" +
            "            \"claims\": {\"claim1\": \"value1\", \"claim2\": \"value2\"},\n" +
            "            \"scopes\": [\"scope1\", \"scope2\"]\n" +
            "            }\n" +
            "        },\n" +
            "        \"domainName\": \"id.execute-api.us-east-1.amazonaws.com\",\n" +
            "        \"domainPrefix\": \"id\",\n" +
            "        \"http\": {\n" +
            "          \"method\": \"POST\",\n" +
            "          \"path\": \"/my/path\",\n" +
            "          \"protocol\": \"HTTP/1.1\",\n" +
            "          \"sourceIp\": \"IP\",\n" +
            "          \"userAgent\": \"agent\"\n" +
            "        },\n" +
            "        \"requestId\": \"id\",\n" +
            "        \"routeKey\": \"$default\",\n" +
            "        \"stage\": \"$default\",\n" +
            "        \"time\": \"12/Mar/2020:19:03:58 +0000\",\n" +
            "        \"timeEpoch\": 1583348638390\n" +
            "      },\n" +
            "      \"body\": \"Hello from Lambda\",\n" +
            "      \"isBase64Encoded\": false,\n" +
            "      \"stageVariables\": {\"stageVariable1\": \"value1\", \"stageVariable2\": \"value2\"}\n" +
            "    }\n";
    private static final String NO_AUTH_PROXY = "{\n" +
            "      \"version\": \"2.0\",\n" +
            "      \"routeKey\": \"$default\",\n" +
            "      \"rawPath\": \"/my/path\",\n" +
            "      \"rawQueryString\": \"parameter1=value1&parameter1=value2&parameter2=value\",\n" +
            "      \"cookies\": [ \"cookie1\", \"cookie2\" ],\n" +
            "      \"headers\": {\n" +
            "        \"Header1\": \"value1\",\n" +
            "        \"Header2\": \"value2\"\n" +
            "      },\n" +
            "      \"queryStringParameters\": { \"parameter1\": \"value1,value2\", \"parameter2\": \"value\" },\n" +
            "      \"requestContext\": {\n" +
            "        \"accountId\": \"123456789012\",\n" +
            "        \"apiId\": \"api-id\",\n" +
            "        \"authorizer\": {\n " +
            "        },\n" +
            "        \"domainName\": \"id.execute-api.us-east-1.amazonaws.com\",\n" +
            "        \"domainPrefix\": \"id\",\n" +
            "        \"http\": {\n" +
            "          \"method\": \"POST\",\n" +
            "          \"path\": \"/my/path\",\n" +
            "          \"protocol\": \"HTTP/1.1\",\n" +
            "          \"sourceIp\": \"IP\",\n" +
            "          \"userAgent\": \"agent\"\n" +
            "        },\n" +
            "        \"requestId\": \"id\",\n" +
            "        \"routeKey\": \"$default\",\n" +
            "        \"stage\": \"$default\",\n" +
            "        \"time\": \"12/Mar/2020:19:03:58 +0000\",\n" +
            "        \"timeEpoch\": 1583348638390\n" +
            "      },\n" +
            "      \"body\": \"Hello from Lambda\",\n" +
            "      \"isBase64Encoded\": true,\n" +
            "      \"stageVariables\": {\"stageVariable1\": \"value1\", \"stageVariable2\": \"value2\"}\n" +
            "    }\n";

    @Test
    public void deserialize_fromJsonString_authorizerPopulatedCorrectly() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(BASE_PROXY_REQUEST, HttpApiV2ProxyRequest.class);
            assertTrue(req.getRequestContext().getAuthorizer().getJwtAuthorizer().getClaims().containsKey("claim1"));
            assertEquals(2, req.getRequestContext().getAuthorizer().getJwtAuthorizer().getScopes().size());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    public void deserialize_fromJsonString_authorizerEmptyMap() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(NO_AUTH_PROXY, HttpApiV2ProxyRequest.class);
            assertNotNull(req.getRequestContext().getAuthorizer());
            assertFalse(req.getRequestContext().getAuthorizer().isJwt());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    public void deserialize_fromJsonString_isBase64EncodedPopulates() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(BASE_PROXY_REQUEST, HttpApiV2ProxyRequest.class);
            assertFalse(req.isBase64Encoded());
            req = LambdaContainerHandler.getObjectMapper().readValue(NO_AUTH_PROXY, HttpApiV2ProxyRequest.class);
            assertTrue(req.isBase64Encoded());
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    public void serialize_toJsonString_authorizerPopulatesCorrectly() {
        HttpApiV2ProxyRequest req = new HttpApiV2ProxyRequest();
        req.setBase64Encoded(false);
        req.setRequestContext(new HttpApiV2ProxyRequestContext());
        req.getRequestContext().setAuthorizer(new HttpApiV2AuthorizerMap());
        req.getRequestContext().getAuthorizer().putJwtAuthorizer(new HttpApiV2JwtAuthorizer());
        ArrayList<String> scopes = new ArrayList<>();
        scopes.add("first");
        scopes.add("second");
        req.getRequestContext().getAuthorizer().getJwtAuthorizer().setScopes(scopes);

        try {
            String reqString = LambdaContainerHandler.getObjectMapper().writeValueAsString(req);
            assertTrue(reqString.contains("\"scopes\":[\"first\",\"second\"]"));
            assertTrue(reqString.contains("\"authorizer\":{\"jwt\":{"));
            assertTrue(reqString.contains("\"isBase64Encoded\":false"));
        } catch (JsonProcessingException e) {
            e.printStackTrace();
            fail("Exception while serializing request" + e.getMessage());
        }
    }
}
