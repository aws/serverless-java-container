package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.junit.Test;

import java.io.IOException;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

import static org.junit.Assert.*;

public class CognitoAuthorizerClaimsTest {

    private static final String USERNAME = "test_username";
    private static final String SUB = "42df3b02-29f1-4779-a3e5-eff92ff280b2";
    private static final String AUD = "2k3no2j1rjjbqaskc4bk0ub29b";
    private static final String EMAIL = "testemail@test.com";

    private static final String EXP_TIME = "Mon Apr 17 23:12:49 UTC 2017";
    private static final String ISSUE_TIME = "Mon Apr 17 22:12:49 UTC 2017";
    static final DateTimeFormatter TOKEN_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy").withLocale(Locale.ENGLISH);

    private static final String USER_POOLS_REQUEST = "{\n"
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
                                                     + "            \"claims\": {\n"
                                                     + "                \"sub\": \"" + SUB + "\",\n"
                                                     + "                \"aud\": \"" + AUD + "\",\n"
                                                     + "                \"email_verified\": \"true\",\n"
                                                     + "                \"token_use\": \"id\",\n"
                                                     + "                \"auth_time\": \"1492467169\",\n"
                                                     + "                \"iss\": \"https://cognito-idp.us-east-2.amazonaws.com/us-east-2_xxXXxxXX\",\n"
                                                     + "                \"cognito:username\": \"" + USERNAME + "\",\n"
                                                     + "                \"exp\": \"" + EXP_TIME + "\",\n"
                                                     + "                \"iat\": \"" + ISSUE_TIME + "\",\n"
                                                     + "                \"email\": \"" + EMAIL + "\"\n"
                                                     + "            }\n"
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
    public void claims_serialize_validJsonString() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder().fromJsonString(USER_POOLS_REQUEST).build();

            assertEquals(USERNAME, req.getRequestContext().getAuthorizer().getClaims().getUsername());
            assertEquals(EMAIL, req.getRequestContext().getAuthorizer().getClaims().getEmail());
            assertTrue(req.getRequestContext().getAuthorizer().getClaims().isEmailVerified());
        } catch (IOException e) {
            fail();
        }
    }

    @Test
    public void claims_dateParse_issueTime() {
        try {
            AwsProxyRequest req = new AwsProxyRequestBuilder().fromJsonString(USER_POOLS_REQUEST).build();

            assertEquals(EXP_TIME, req.getRequestContext().getAuthorizer().getClaims().getExpiration());
            assertNotNull(req.getRequestContext().getAuthorizer().getClaims().getExpiration());

            ZonedDateTime expTime = ZonedDateTime.from(TOKEN_DATE_FORMATTER.parse(EXP_TIME));
            ZonedDateTime issueTime = ZonedDateTime.from(TOKEN_DATE_FORMATTER.parse(ISSUE_TIME));
            assertEquals(expTime, ZonedDateTime.from(TOKEN_DATE_FORMATTER.parse(req.getRequestContext().getAuthorizer().getClaims().getExpiration())));

            assertEquals(expTime, issueTime.plusHours(1));
        } catch (IOException e) {
            e.printStackTrace();
            fail();
        }
    }
}