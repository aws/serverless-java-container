package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.junit.Test;

import java.io.IOException;

import static org.junit.Assert.*;

public class ApiGatewayAuthorizerContextTest {
    private static final String FIELD_NAME_1 = "CUSTOM_FIELD_1";
    private static final String FIELD_NAME_2 = "CUSTOM_FIELD_2";
    private static final String FIELD_VALUE_1 = "VALUE_1";
    private static final String FIELD_VALUE_2 = "VALUE_2";
    private static final String PRINCIPAL = "xxxxx";

    private static final String AUTHORIZER_REQUEST = "{\n"
                                                     + "    \"resource\": \"/restaurants\",\n"
                                                     + "    \"path\": \"/restaurants\",\n"
                                                     + "    \"httpMethod\": \"GET\",\n"
                                                     + "    \"headers\": {\n"
                                                     + "        \"Accept\": \"*/*\",\n"
                                                     + "        \"Authorization\": \"eyJraWQiOiJKSm9VQUtrRThcL3NTU3Rwa3dPZTFWN2dvak1xS0k1NU8zTzB4WVgwMGNRdz0iLCJhbGciOiJSUzI1NiJ9.eyJzdWIiOiI0MmRmM2IwMi0yOWYxLTQ3NzktYTNlNS1lZmY5MmZmMjgwYjIiLCJhdWQiOiIyazNubzJqMXJqamJxYXNrYzRiazB1YjI5YiIsImVtYWlsX3ZlcmlmaWVkIjp0cnVlLCJ0b2tlbl91c2UiOiJpZCIsImF1dGhfdGltZSI6MTQ5MjQ2NzE2OSwiaXNzIjoiaHR0cHM6XC9cL2NvZ25pdG8taWRwLnVzLWVhc3QtMi5hbWF6b25hd3MuY29tXC91cy1lYXN0LTJfQWR4NVpIZVBnIiwiY29nbml0bzp1c2VybmFtZSI6InNhcGVzc2kiLCJleHAiOjE0OTI0NzA3NjksImlhdCI6MTQ5MjQ2NzE2OSwiZW1haWwiOiJidWxpYW5pc0BhbWF6b24uY29tIn0.aTODUMNib_pQhad1aWTHrlz7kwA5QkcvZptcbLFY5BuNqpr9zsK14EhHRvmvflK4MMQaxCE5Cxa9joR9g-HCmmF1usZhXO4Q2iyEWcBk0whjn3CnC55k6yEuMv6y9krts0YHSamsRkhW7wnCpuLmk2KgzHTfyt6oQ1qbg9QE8l9LRhjCHLnujlLIQaG9p9UfJVf-uGSg1k_bCyzl48lqkc7LDwqDZCHXGf1RYRQLg5jphXF_tjByDk_0t9Ah7pX2nFwl0SUz74enG8emq58g4pemeVekb9Mw0wyD-B5TWeGVs_nvmC3q4jgxMyJy3Xq4Ggd9qSgIN_Khdg3Q26F2bA\",\n"
                                                     + "        \"CloudFront-Forwarded-Proto\": \"https\"\n"
                                                     + "    },\n"
                                                     + "    \"queryStringParameters\": null,\n"
                                                     + "    \"pathParameters\": null,\n"
                                                     + "    \"stageVariables\": null,\n"
                                                     + "    \"requestContext\": {\n"
                                                     + "        \"accountId\": \"XXXXXXXXXXXXXX\",\n"
                                                     + "        \"resourceId\": \"xxxxx\",\n"
                                                     + "        \"stage\": \"dev\",\n"
                                                     + "        \"authorizer\": {\n"
                                                     + "            \"principalId\": \"" + PRINCIPAL + "\",\n"
                                                     + "            \"" + FIELD_NAME_1 + "\": \"" + FIELD_VALUE_1 + "\","
                                                     + "            \"" + FIELD_NAME_2 + "\": \"" + FIELD_VALUE_2 + "\""
                                                     + "        },\n"
                                                     + "        \"requestId\": \"ad0a33ba-23bc-11e7-9b7d-235a67eb05bd\",\n"
                                                     + "        \"identity\": {\n"
                                                     + "            \"cognitoIdentityPoolId\": null,\n"
                                                     + "            \"accountId\": null,\n"
                                                     + "            \"cognitoIdentityId\": null,\n"
                                                     + "            \"caller\": null,\n"
                                                     + "            \"apiKey\": null,\n"
                                                     + "            \"sourceIp\": \"54.240.196.171\",\n"
                                                     + "            \"accessKey\": null,\n"
                                                     + "            \"cognitoAuthenticationType\": null,\n"
                                                     + "            \"cognitoAuthenticationProvider\": null,\n"
                                                     + "            \"userArn\": null,\n"
                                                     + "            \"userAgent\": \"PostmanRuntime/3.0.1\",\n"
                                                     + "            \"user\": null\n"
                                                     + "        },\n"
                                                     + "        \"resourcePath\": \"/restaurants\",\n"
                                                     + "        \"httpMethod\": \"GET\",\n"
                                                     + "        \"apiId\": \"xxxxxxxx\"\n"
                                                     + "    },\n"
                                                     + "    \"body\": null,\n"
                                                     + "    \"isBase64Encoded\": false\n"
                                                     + "}";

    @Test
    public void authorizerContext_serialize_customValues() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder().fromJsonString(AUTHORIZER_REQUEST).build();

            assertNotNull(req.getRequestContext().getAuthorizer().getContextValue(FIELD_NAME_1));
            assertNotNull(req.getRequestContext().getAuthorizer().getContextValue(FIELD_NAME_2));
            assertEquals(FIELD_VALUE_1, req.getRequestContext().getAuthorizer().getContextValue(FIELD_NAME_1));
            assertEquals(FIELD_VALUE_2, req.getRequestContext().getAuthorizer().getContextValue(FIELD_NAME_2));
            assertEquals(PRINCIPAL, req.getRequestContext().getAuthorizer().getPrincipalId());
            assertNull(req.getRequestContext().getAuthorizer().getContextValue("principalId"));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}
