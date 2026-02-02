package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import tools.jackson.core.JacksonException;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;

import static org.junit.jupiter.api.Assertions.*;

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
    private static final String LAMBDA_AUTHORIZER = "{\n" +
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
            "        \"authorizer\": {  \"lambda\": {\n" +
            "           \"arrayKey\": [\n" +
            "                \"value1\",\n" +
            "              \"value2\"\n" +
            "            ],\n" +
            "            \"booleanKey\": true,\n" +
            "          \"mapKey\": {\n" +
            "                \"value1\": \"value2\"\n" +
            "                 },\n" +
            "             \"numberKey\": 1,\n" +
            "             \"stringKey\": \"value\"\n" +
            "           }" +
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
    private static final String IAM_AUTHORIZER = "{\n" +
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
            "        \"authorizer\": {  \"iam\": {\n" +
            "           \"accessKey\": \"AKIAIOSFODNN7EXAMPLE\",\n" +
            "           \"accountId\": \"123456789012\",\n" +
            "           \"callerId\": \"AIDACKCEVSQ6C2EXAMPLE\",\n" +
            "           \"cognitoIdentity\": null,\n" +
            "           \"principalOrgId\": \"AIDACKCEVSQORGEXAMPLE\",\n" +
            "           \"userArn\": \"arn:aws:iam::111122223333:user/example-user\",\n" +
            "           \"userId\": \"AIDACOSFODNN7EXAMPLE2\"\n" +
            "           }" +
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

    @Test
    void deserialize_fromJsonString_authorizerPopulatedCorrectly() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(BASE_PROXY_REQUEST,
                    HttpApiV2ProxyRequest.class);
            assertTrue(req.getRequestContext().getAuthorizer().getJwtAuthorizer().getClaims().containsKey("claim1"));
            assertEquals(2, req.getRequestContext().getAuthorizer().getJwtAuthorizer().getScopes().size());
            assertEquals(RequestSource.API_GATEWAY, req.getRequestSource());
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void deserialize_fromJsonString_authorizerEmptyMap() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(NO_AUTH_PROXY,
                    HttpApiV2ProxyRequest.class);
            assertNotNull(req.getRequestContext().getAuthorizer());
            assertFalse(req.getRequestContext().getAuthorizer().isJwt());
            assertFalse(req.getRequestContext().getAuthorizer().isLambda());
            assertFalse(req.getRequestContext().getAuthorizer().isIam());
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void deserialize_fromJsonString_lambdaAuthorizer() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(LAMBDA_AUTHORIZER,
                    HttpApiV2ProxyRequest.class);
            assertNotNull(req.getRequestContext().getAuthorizer());
            assertFalse(req.getRequestContext().getAuthorizer().isJwt());
            assertTrue(req.getRequestContext().getAuthorizer().isLambda());
            assertEquals(5, req.getRequestContext().getAuthorizer().getLambdaAuthorizerContext().size());
            assertEquals(1, req.getRequestContext().getAuthorizer().getLambdaAuthorizerContext().get("numberKey"));
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void deserialize_fromJsonString_iamAuthorizer() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(IAM_AUTHORIZER,
                    HttpApiV2ProxyRequest.class);
            assertNotNull(req.getRequestContext().getAuthorizer());
            assertFalse(req.getRequestContext().getAuthorizer().isJwt());
            assertFalse(req.getRequestContext().getAuthorizer().isLambda());
            assertTrue(req.getRequestContext().getAuthorizer().isIam());
            assertEquals("AKIAIOSFODNN7EXAMPLE",
                    req.getRequestContext().getAuthorizer().getIamAuthorizer().getAccessKey());
            assertEquals("123456789012", req.getRequestContext().getAuthorizer().getIamAuthorizer().getAccountId());
            assertEquals("AIDACKCEVSQ6C2EXAMPLE",
                    req.getRequestContext().getAuthorizer().getIamAuthorizer().getCallerId());
            assertNull(req.getRequestContext().getAuthorizer().getIamAuthorizer().getCognitoIdentity());
            assertEquals("AIDACKCEVSQORGEXAMPLE",
                    req.getRequestContext().getAuthorizer().getIamAuthorizer().getPrincipalOrgId());
            assertEquals("arn:aws:iam::111122223333:user/example-user",
                    req.getRequestContext().getAuthorizer().getIamAuthorizer().getUserArn());
            assertEquals("AIDACOSFODNN7EXAMPLE2",
                    req.getRequestContext().getAuthorizer().getIamAuthorizer().getUserId());
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void deserialize_fromJsonString_isBase64EncodedPopulates() {
        try {
            HttpApiV2ProxyRequest req = LambdaContainerHandler.getObjectMapper().readValue(BASE_PROXY_REQUEST,
                    HttpApiV2ProxyRequest.class);
            assertFalse(req.isBase64Encoded());
            req = LambdaContainerHandler.getObjectMapper().readValue(NO_AUTH_PROXY, HttpApiV2ProxyRequest.class);
            assertTrue(req.isBase64Encoded());
            assertEquals(RequestSource.API_GATEWAY, req.getRequestSource());
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while parsing request" + e.getMessage());
        }
    }

    @Test
    void serialize_toJsonString_authorizerPopulatesCorrectly() {
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
        } catch (JacksonException e) {
            e.printStackTrace();
            fail("Exception while serializing request" + e.getMessage());
        }
    }
}