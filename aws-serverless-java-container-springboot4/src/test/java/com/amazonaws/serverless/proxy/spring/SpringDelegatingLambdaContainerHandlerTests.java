package com.amazonaws.serverless.proxy.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.*;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.CollectionUtils;

import com.amazonaws.serverless.proxy.spring.servletapp.MessageData;
import com.amazonaws.serverless.proxy.spring.servletapp.ServletApplication;
import com.amazonaws.serverless.proxy.spring.servletapp.UserData;
import tools.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.HttpHeaders;

@SuppressWarnings("rawtypes")
public class SpringDelegatingLambdaContainerHandlerTests {

    private static final String API_GATEWAY_EVENT = """
            {
                "version": "1.0",
                "resource": "$default",
                "path": "/async",
                "httpMethod": "POST",
                "headers": {
                    "Content-Length": "45",
                    "Content-Type": "application/json",
                    "Host": "i76bfh111.execute-api.eu-west-3.amazonaws.com",
                    "User-Agent": "curl/7.79.1",
                    "X-Amzn-Trace-Id": "Root=1-64087690-2151375b219d3ba3389ea84e",
                    "X-Forwarded-For": "109.210.252.44",
                    "X-Forwarded-Port": "443",
                    "X-Forwarded-Proto": "https",
                    "accept": "*/*"
                },
                "multiValueHeaders": {
                    "Content-Length": [
                        "45"
                    ],
                    "Content-Type": [
                        "application/json"
                    ],
                    "Host": [
                        "i76bfhczs0.execute-api.eu-west-3.amazonaws.com"
                    ],
                    "User-Agent": [
                        "curl/7.79.1"
                    ],
                    "X-Amzn-Trace-Id": [
                        "Root=1-64087690-2151375b219d3ba3389ea84e"
                    ],
                    "X-Forwarded-For": [
                        "109.210.252.44"
                    ],
                    "X-Forwarded-Port": [
                        "443"
                    ],
                    "X-Forwarded-Proto": [
                        "https"
                    ],
                    "accept": [
                        "*/*"
                    ]
                },
                "queryStringParameters": {
                    "abc": "xyz",
                    "name": "Ricky",
                    "foo": "baz"
                },
                "multiValueQueryStringParameters": {
                    "abc": [
                        "xyz"
                    ],
                    "name": [
                        "Ricky"
                    ],
                    "foo": [
                        "bar",
                        "baz"
                    ]
                },
                "requestContext": {
                    "accountId": "123456789098",
                    "apiId": "i76bfhczs0",
                    "domainName": "i76bfhc111.execute-api.eu-west-3.amazonaws.com",
                    "domainPrefix": "i76bfhczs0",
                    "extendedRequestId": "Bdd2ngt5iGYEMIg=",
                    "httpMethod": "POST",
                    "identity": {
                        "accessKey": null,
                        "accountId": null,
                        "caller": null,
                        "cognitoAmr": null,
                        "cognitoAuthenticationProvider": null,
                        "cognitoAuthenticationType": null,
                        "cognitoIdentityId": null,
                        "cognitoIdentityPoolId": null,
                        "principalOrgId": null,
                        "sourceIp": "109.210.252.44",
                        "user": null,
                        "userAgent": "curl/7.79.1",
                        "userArn": null
                    },
                    "path": "/pets",
                    "protocol": "HTTP/1.1",
                    "requestId": "Bdd2ngt5iGYEMIg=",
                    "requestTime": "08/Mar/2023:11:50:40 +0000",
                    "requestTimeEpoch": 1678276240455,
                    "resourceId": "$default",
                    "resourcePath": "$default",
                    "stage": "$default"
                },
                "pathParameters": null,
                "stageVariables": null,
                "body": "{\\"name\\":\\"bob\\"}",
                "isBase64Encoded": false
            }""";

    private static final String API_GATEWAY_EVENT_V2 = """
            {
              "version": "2.0",
              "routeKey": "$default",
              "rawPath": "/my/path",
              "rawQueryString": "parameter1=value1&parameter1=value2&name=Ricky&parameter2=value",
              "cookies": [
                "cookie1",
                "cookie2"
              ],
              "headers": {
                "header1": "value1",
                "header2": "value1,value2"
              },
              "queryStringParameters": {
                "parameter1": "value1,value2",
                "name": "Ricky",
                "parameter2": "value"
              },
              "requestContext": {
                "accountId": "123456789012",
                "apiId": "api-id",
                "authentication": {
                  "clientCert": {
                    "clientCertPem": "CERT_CONTENT",
                    "subjectDN": "www.example.com",
                    "issuerDN": "Example issuer",
                    "serialNumber": "a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1",
                    "validity": {
                      "notBefore": "May 28 12:30:02 2019 GMT",
                      "notAfter": "Aug  5 09:36:04 2021 GMT"
                    }
                  }
                },
                "authorizer": {
                  "jwt": {
                    "claims": {
                      "claim1": "value1",
                      "claim2": "value2"
                    },
                    "scopes": [
                      "scope1",
                      "scope2"
                    ]
                  }
                },
                "domainName": "id.execute-api.us-east-1.amazonaws.com",
                "domainPrefix": "id",
                "http": {
                  "method": "POST",
                  "path": "/my/path",
                  "protocol": "HTTP/1.1",
                  "sourceIp": "IP",
                  "userAgent": "agent"
                },
                "requestId": "id",
                "routeKey": "$default",
                "stage": "$default",
                "time": "12/Mar/2020:19:03:58 +0000",
                "timeEpoch": 1583348638390
              },
              "body": "Hello from Lambda",
              "pathParameters": {
                "parameter1": "value1"
              },
              "isBase64Encoded": false,
              "stageVariables": {
                "stageVariable1": "value1",
                "stageVariable2": "value2"
              }
            }""";

    private SpringDelegatingLambdaContainerHandler handler;

    private ObjectMapper mapper = new ObjectMapper();

    public void initServletAppTest() throws ContainerInitializationException {
        this.handler = new SpringDelegatingLambdaContainerHandler(ServletApplication.class);
    }

    public static Collection<String> data() {
        return Arrays.asList(API_GATEWAY_EVENT, API_GATEWAY_EVENT_V2);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void validateComplesrequest(String jsonEvent) throws Exception {
        initServletAppTest();
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", 
        		"/foo/male/list/24", "{\"name\":\"bob\"}", false,null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(200, result.get("statusCode"));
        String[] responseBody = ((String) result.get("body")).split("/");
        assertEquals("male", responseBody[0]);
        assertEquals("24", responseBody[1]);
        assertEquals("Ricky", responseBody[2]);
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testValidate400(String jsonEvent) throws Exception {
        initServletAppTest();
        UserData ud = new UserData();
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/validate", mapper.writeValueAsString(ud),false, null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(400, result.get("statusCode"));
        assertEquals("3", result.get("body"));
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testValidate200(String jsonEvent) throws Exception {
        initServletAppTest();
        UserData ud = new UserData();
        ud.setFirstName("bob");
        ud.setLastName("smith");
        ud.setEmail("foo@bar.com");
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/validate", mapper.writeValueAsString(ud),false, null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(200, result.get("statusCode"));
        assertEquals("VALID", result.get("body"));
    }

    @MethodSource("data")
    @ParameterizedTest
    public void testValidate200Base64(String jsonEvent) throws Exception {
        initServletAppTest();
        UserData ud = new UserData();
        ud.setFirstName("bob");
        ud.setLastName("smith");
        ud.setEmail("foo@bar.com");
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/validate",
                Base64.getMimeEncoder().encodeToString(mapper.writeValueAsString(ud).getBytes()),true, null));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(200, result.get("statusCode"));
        assertEquals("VALID", result.get("body"));
    }

    @MethodSource("data")
    @ParameterizedTest
    public void messageObject_parsesObject_returnsCorrectMessage(String jsonEvent) throws Exception {
        initServletAppTest();
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/message",
                mapper.writeValueAsString(new MessageData("test message")),false, null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(200, result.get("statusCode"));
        assertEquals("test message", result.get("body"));
    }

    @MethodSource("data")
    @ParameterizedTest
    public void voidPost_returns200(String jsonEvent) throws Exception {
        initServletAppTest();
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST",
                "/void-post", "{\"key\":\"value\"}", false, null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals(200, result.get("statusCode"));
    }

    @SuppressWarnings({"unchecked" })
    @MethodSource("data")
    @ParameterizedTest
    void messageObject_propertiesInContentType_returnsCorrectMessage(String jsonEvent) throws Exception {
        initServletAppTest();

        Map headers = new HashMap<>();
        headers.put(HttpHeaders.CONTENT_TYPE, "application/json;v=1");
        headers.put(HttpHeaders.ACCEPT, "application/json;v=1");
        InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/message",
                mapper.writeValueAsString(new MessageData("test message")),false, headers));

        ByteArrayOutputStream output = new ByteArrayOutputStream();
        handler.handleRequest(targetStream, output, null);
        Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
        assertEquals("test message", result.get("body"));
    }

    private byte[] generateHttpRequest(String jsonEvent, String method, String path, String body,boolean isBase64Encoded, Map headers) throws Exception {
        Map requestMap = mapper.readValue(jsonEvent, Map.class);
        if (requestMap.get("version").equals("2.0")) {
            return generateHttpRequest2(requestMap, method, path, body, isBase64Encoded,headers);
        }
        return generateHttpRequest(requestMap, method, path, body,isBase64Encoded, headers);
    }

    @SuppressWarnings({ "unchecked"})
    private byte[] generateHttpRequest(Map requestMap, String method, String path, String body,boolean isBase64Encoded, Map headers) throws Exception {
        requestMap.put("path", path);
        requestMap.put("httpMethod", method);
        requestMap.put("body", body);
        requestMap.put("isBase64Encoded", isBase64Encoded);
        if (!CollectionUtils.isEmpty(headers)) {
            requestMap.put("headers", headers);
        }
        return mapper.writeValueAsBytes(requestMap);
    }

    @SuppressWarnings({ "unchecked"})
    private byte[] generateHttpRequest2(Map requestMap, String method, String path, String body,boolean isBase64Encoded, Map headers) throws Exception {
        Map map = mapper.readValue(API_GATEWAY_EVENT_V2, Map.class);
        Map http = (Map) ((Map) map.get("requestContext")).get("http");
        http.put("path", path);
        http.put("method", method);
        map.put("body", body);
        map.put("isBase64Encoded", isBase64Encoded);
        if (!CollectionUtils.isEmpty(headers)) {
            map.put("headers", headers);
        }
        return mapper.writeValueAsBytes(map);
    }
}
