package com.amazonaws.serverless.proxy.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.util.CollectionUtils;

import com.amazonaws.serverless.proxy.spring.servletapp.MessageData;
import com.amazonaws.serverless.proxy.spring.servletapp.ServletApplication;
import com.amazonaws.serverless.proxy.spring.servletapp.UserData;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.ws.rs.core.HttpHeaders;

@SuppressWarnings("rawtypes")
public class SpringDelegatingLambdaContainerHandlerTests {

	private static String API_GATEWAY_EVENT = "{\n"
			+ "    \"version\": \"1.0\",\n"
			+ "    \"resource\": \"$default\",\n"
			+ "    \"path\": \"/async\",\n"
			+ "    \"httpMethod\": \"POST\",\n"
			+ "    \"headers\": {\n"
			+ "        \"Content-Length\": \"45\",\n"
			+ "        \"Content-Type\": \"application/json\",\n"
			+ "        \"Host\": \"i76bfh111.execute-api.eu-west-3.amazonaws.com\",\n"
			+ "        \"User-Agent\": \"curl/7.79.1\",\n"
			+ "        \"X-Amzn-Trace-Id\": \"Root=1-64087690-2151375b219d3ba3389ea84e\",\n"
			+ "        \"X-Forwarded-For\": \"109.210.252.44\",\n"
			+ "        \"X-Forwarded-Port\": \"443\",\n"
			+ "        \"X-Forwarded-Proto\": \"https\",\n"
			+ "        \"accept\": \"*/*\"\n"
			+ "    },\n"
			+ "    \"multiValueHeaders\": {\n"
			+ "        \"Content-Length\": [\n"
			+ "            \"45\"\n"
			+ "        ],\n"
			+ "        \"Content-Type\": [\n"
			+ "            \"application/json\"\n"
			+ "        ],\n"
			+ "        \"Host\": [\n"
			+ "            \"i76bfhczs0.execute-api.eu-west-3.amazonaws.com\"\n"
			+ "        ],\n"
			+ "        \"User-Agent\": [\n"
			+ "            \"curl/7.79.1\"\n"
			+ "        ],\n"
			+ "        \"X-Amzn-Trace-Id\": [\n"
			+ "            \"Root=1-64087690-2151375b219d3ba3389ea84e\"\n"
			+ "        ],\n"
			+ "        \"X-Forwarded-For\": [\n"
			+ "            \"109.210.252.44\"\n"
			+ "        ],\n"
			+ "        \"X-Forwarded-Port\": [\n"
			+ "            \"443\"\n"
			+ "        ],\n"
			+ "        \"X-Forwarded-Proto\": [\n"
			+ "            \"https\"\n"
			+ "        ],\n"
			+ "        \"accept\": [\n"
			+ "            \"*/*\"\n"
			+ "        ]\n"
			+ "    },\n"
			+ "    \"queryStringParameters\": {\n"
			+ "        \"abc\": \"xyz\",\n"
			+ "        \"foo\": \"baz\"\n"
			+ "    },\n"
			+ "    \"multiValueQueryStringParameters\": {\n"
			+ "        \"abc\": [\n"
			+ "            \"xyz\"\n"
			+ "        ],\n"
			+ "        \"foo\": [\n"
			+ "            \"bar\",\n"
			+ "            \"baz\"\n"
			+ "        ]\n"
			+ "    },\n"
			+ "    \"requestContext\": {\n"
			+ "        \"accountId\": \"123456789098\",\n"
			+ "        \"apiId\": \"i76bfhczs0\",\n"
			+ "        \"domainName\": \"i76bfhc111.execute-api.eu-west-3.amazonaws.com\",\n"
			+ "        \"domainPrefix\": \"i76bfhczs0\",\n"
			+ "        \"extendedRequestId\": \"Bdd2ngt5iGYEMIg=\",\n"
			+ "        \"httpMethod\": \"POST\",\n"
			+ "        \"identity\": {\n"
			+ "            \"accessKey\": null,\n"
			+ "            \"accountId\": null,\n"
			+ "            \"caller\": null,\n"
			+ "            \"cognitoAmr\": null,\n"
			+ "            \"cognitoAuthenticationProvider\": null,\n"
			+ "            \"cognitoAuthenticationType\": null,\n"
			+ "            \"cognitoIdentityId\": null,\n"
			+ "            \"cognitoIdentityPoolId\": null,\n"
			+ "            \"principalOrgId\": null,\n"
			+ "            \"sourceIp\": \"109.210.252.44\",\n"
			+ "            \"user\": null,\n"
			+ "            \"userAgent\": \"curl/7.79.1\",\n"
			+ "            \"userArn\": null\n"
			+ "        },\n"
			+ "        \"path\": \"/pets\",\n"
			+ "        \"protocol\": \"HTTP/1.1\",\n"
			+ "        \"requestId\": \"Bdd2ngt5iGYEMIg=\",\n"
			+ "        \"requestTime\": \"08/Mar/2023:11:50:40 +0000\",\n"
			+ "        \"requestTimeEpoch\": 1678276240455,\n"
			+ "        \"resourceId\": \"$default\",\n"
			+ "        \"resourcePath\": \"$default\",\n"
			+ "        \"stage\": \"$default\"\n"
			+ "    },\n"
			+ "    \"pathParameters\": null,\n"
			+ "    \"stageVariables\": null,\n"
			+ "    \"body\": \"{\\\"name\\\":\\\"bob\\\"}\",\n"
			+ "    \"isBase64Encoded\": false\n"
			+ "}";

	private static String API_GATEWAY_EVENT_V2 = "{\n" +
			"  \"version\": \"2.0\",\n" +
			"  \"routeKey\": \"$default\",\n" +
			"  \"rawPath\": \"/my/path\",\n" +
			"  \"rawQueryString\": \"parameter1=value1&parameter1=value2&parameter2=value\",\n" +
			"  \"cookies\": [\n" +
			"    \"cookie1\",\n" +
			"    \"cookie2\"\n" +
			"  ],\n" +
			"  \"headers\": {\n" +
			"    \"header1\": \"value1\",\n" +
			"    \"header2\": \"value1,value2\"\n" +
			"  },\n" +
			"  \"queryStringParameters\": {\n" +
			"    \"parameter1\": \"value1,value2\",\n" +
			"    \"parameter2\": \"value\"\n" +
			"  },\n" +
			"  \"requestContext\": {\n" +
			"    \"accountId\": \"123456789012\",\n" +
			"    \"apiId\": \"api-id\",\n" +
			"    \"authentication\": {\n" +
			"      \"clientCert\": {\n" +
			"        \"clientCertPem\": \"CERT_CONTENT\",\n" +
			"        \"subjectDN\": \"www.example.com\",\n" +
			"        \"issuerDN\": \"Example issuer\",\n" +
			"        \"serialNumber\": \"a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1:a1\",\n" +
			"        \"validity\": {\n" +
			"          \"notBefore\": \"May 28 12:30:02 2019 GMT\",\n" +
			"          \"notAfter\": \"Aug  5 09:36:04 2021 GMT\"\n" +
			"        }\n" +
			"      }\n" +
			"    },\n" +
			"    \"authorizer\": {\n" +
			"      \"jwt\": {\n" +
			"        \"claims\": {\n" +
			"          \"claim1\": \"value1\",\n" +
			"          \"claim2\": \"value2\"\n" +
			"        },\n" +
			"        \"scopes\": [\n" +
			"          \"scope1\",\n" +
			"          \"scope2\"\n" +
			"        ]\n" +
			"      }\n" +
			"    },\n" +
			"    \"domainName\": \"id.execute-api.us-east-1.amazonaws.com\",\n" +
			"    \"domainPrefix\": \"id\",\n" +
			"    \"http\": {\n" +
			"      \"method\": \"POST\",\n" +
			"      \"path\": \"/my/path\",\n" +
			"      \"protocol\": \"HTTP/1.1\",\n" +
			"      \"sourceIp\": \"IP\",\n" +
			"      \"userAgent\": \"agent\"\n" +
			"    },\n" +
			"    \"requestId\": \"id\",\n" +
			"    \"routeKey\": \"$default\",\n" +
			"    \"stage\": \"$default\",\n" +
			"    \"time\": \"12/Mar/2020:19:03:58 +0000\",\n" +
			"    \"timeEpoch\": 1583348638390\n" +
			"  },\n" +
			"  \"body\": \"Hello from Lambda\",\n" +
			"  \"pathParameters\": {\n" +
			"    \"parameter1\": \"value1\"\n" +
			"  },\n" +
			"  \"isBase64Encoded\": false,\n" +
			"  \"stageVariables\": {\n" +
			"    \"stageVariable1\": \"value1\",\n" +
			"    \"stageVariable2\": \"value2\"\n" +
			"  }\n" +
			"}";

	private SpringDelegatingLambdaContainerHandler handler;

	private ObjectMapper mapper = new ObjectMapper();

	public void initServletAppTest()  {
		this.handler = new SpringDelegatingLambdaContainerHandler(ServletApplication.class);
	}

	public static Collection<String> data() {
        return Arrays.asList(new String[]{API_GATEWAY_EVENT, API_GATEWAY_EVENT_V2});
    }

	@MethodSource("data")
	@ParameterizedTest
	public void testAsyncPost(String jsonEvent) throws Exception {
		initServletAppTest();
		InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/async", "{\"name\":\"bob\"}", null));
		ByteArrayOutputStream output = new ByteArrayOutputStream();
		handler.handleRequest(targetStream, output, null);
		Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
		assertEquals(200, result.get("statusCode"));
		assertEquals("{\"name\":\"BOB\"}", result.get("body"));
	}

	@MethodSource("data")
	@ParameterizedTest
	public void testValidate400(String jsonEvent) throws Exception {
		initServletAppTest();
		UserData ud = new UserData();
		InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/validate", mapper.writeValueAsString(ud), null));
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
		InputStream targetStream = new ByteArrayInputStream(this.generateHttpRequest(jsonEvent, "POST", "/validate", mapper.writeValueAsString(ud), null));
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
        		mapper.writeValueAsString(new MessageData("test message")), null));
        ByteArrayOutputStream output = new ByteArrayOutputStream();
		handler.handleRequest(targetStream, output, null);
		Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
		assertEquals(200, result.get("statusCode"));
		assertEquals("test message", result.get("body"));
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
				mapper.writeValueAsString(new MessageData("test message")), headers));

		ByteArrayOutputStream output = new ByteArrayOutputStream();
		handler.handleRequest(targetStream, output, null);
		Map result = mapper.readValue(output.toString(StandardCharsets.UTF_8), Map.class);
		assertEquals("test message", result.get("body"));
    }

	private byte[] generateHttpRequest(String jsonEvent, String method, String path, String body, Map headers) throws Exception {
		Map requestMap = mapper.readValue(jsonEvent, Map.class);
		if (requestMap.get("version").equals("2.0")) {
			return generateHttpRequest2(requestMap, method, path, body, headers);
		}
		return generateHttpRequest(requestMap, method, path, body, headers);
	}

	@SuppressWarnings({ "unchecked"})
	private byte[] generateHttpRequest(Map requestMap, String method, String path, String body, Map headers) throws Exception {
		requestMap.put("path", path);
		requestMap.put("httpMethod", method);
		requestMap.put("body", body);
		if (!CollectionUtils.isEmpty(headers)) {
			requestMap.put("headers", headers);
		}
		return mapper.writeValueAsBytes(requestMap);
	}

	@SuppressWarnings({ "unchecked"})
	private byte[] generateHttpRequest2(Map requestMap, String method, String path, String body, Map headers) throws Exception {
		Map map = mapper.readValue(API_GATEWAY_EVENT_V2, Map.class);
		Map http = (Map) ((Map) map.get("requestContext")).get("http");
		http.put("path", path);
		http.put("method", method);
		map.put("body", body);
		if (!CollectionUtils.isEmpty(headers)) {
			map.put("headers", headers);
		}
		return mapper.writeValueAsBytes(map);
	}
}
