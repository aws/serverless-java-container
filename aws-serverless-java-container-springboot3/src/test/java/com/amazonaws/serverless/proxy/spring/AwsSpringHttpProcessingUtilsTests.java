package com.amazonaws.serverless.proxy.spring;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.io.ByteArrayInputStream;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;
import org.springframework.cloud.function.serverless.web.ServerlessServletContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.http.HttpServletRequest;

public class AwsSpringHttpProcessingUtilsTests {
	
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
            "  \"rawPath\": \"/async\",\n" +
            "  \"rawQueryString\": \"parameter1=value1&parameter1=value2&parameter2=value\",\n" +
            "  \"cookies\": [\n" +
            "    \"cookie1\",\n" +
            "    \"cookie2\"\n" +
            "  ],\n" +
            "  \"headers\": {\n" +
            "    \"header1\": \"value1\",\n" +
            "    \"header2\": \"value1,value2\",\n" +
            "    \"User-Agent\": \"curl/7.79.1\",\n" + 
            "    \"X-Forwarded-Port\": \"443\"\n" +
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
            "      \"path\": \"/async\",\n" +
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

	private static final String VPC_LATTICE_V2_EVENT = "{\n" +
			"    \"version\": \"2.0\",\n" +
			"    \"path\": \"/async\",\n" +
			"    \"method\": \"POST\",\n" +
			"    \"headers\": {\n" +
			"      \"x-forwarded-for\": [\n" +
			"        \"10.0.0.39\"\n" +
			"      ],\n" +
			"      \"content-type\": [\n" +
			"        \"application/json\"\n" +
			"      ],\n" +
			"      \"accept-encoding\": [\n" +
			"        \"identity\"\n" +
			"      ],\n" +
			"      \"host\": [\n" +
			"        \"demo-service-057f55fd2927517c9.7d67968.vpc-lattice-svcs.us-west-2.on.aws\"\n" +
			"      ],\n" +
			"      \"content-length\": [\n" +
			"        \"54\"\n" +
			"      ]\n" +
			"    },\n" +
			"    \"body\": \"{\\\"key1\\\": \\\"value1\\\", \\\"key2\\\": \\\"value2\\\", \\\"key3\\\": \\\"value3\\\"}\",\n" +
			"    \"requestContext\": {\n" +
			"      \"serviceNetworkArn\": \"arn:aws:vpc-lattice:us-west-2:422832612717:servicenetwork/sn-0bcd972d368921a67\",\n" +
			"      \"serviceArn\": \"arn:aws:vpc-lattice:us-west-2:422832612717:service/svc-057f55fd2927517c9\",\n" +
			"      \"targetGroupArn\": \"arn:aws:vpc-lattice:us-west-2:422832612717:targetgroup/tg-0c8666d87e4ebad20\",\n" +
			"      \"identity\": {\n" +
			"        \"sourceVpcArn\": \"arn:aws:ec2:us-west-2:422832612717:vpc/vpc-05417e632a7302cb7\"\n" +
			"      },\n" +
			"      \"region\": \"us-west-2\",\n" +
			"      \"timeEpoch\": \"1711801592405546\"\n" +
			"    }\n" +
			"  }";

    private final ObjectMapper mapper = new ObjectMapper();

    public static Collection<String> data() {
        return Arrays.asList(new String[]{API_GATEWAY_EVENT, API_GATEWAY_EVENT_V2});
    }

    @MethodSource("data")
    @ParameterizedTest
	public void validateHttpServletRequestGenerationWithInputStream(String jsonEvent) {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(jsonEvent.getBytes(StandardCharsets.UTF_8));
		ServerlessServletContext servletContext = new ServerlessServletContext();
		HttpServletRequest request = AwsSpringHttpProcessingUtils.generateHttpServletRequest(inputStream, null, servletContext, mapper);
		// spot check some headers
		assertEquals("curl/7.79.1", request.getHeader("User-Agent"));
		assertEquals("443", request.getHeader("X-Forwarded-Port"));
		assertEquals("POST", request.getMethod());
		assertEquals("/async", request.getRequestURI());
	}

    @MethodSource("data")
    @ParameterizedTest
	public void validateHttpServletRequestGenerationWithJson(String jsonEvent) {
		ServerlessServletContext servletContext = new ServerlessServletContext();
		HttpServletRequest request = AwsSpringHttpProcessingUtils.generateHttpServletRequest(jsonEvent, null, servletContext, mapper);
		// spot check some headers
		assertEquals("curl/7.79.1", request.getHeader("User-Agent"));
		assertEquals("443", request.getHeader("X-Forwarded-Port"));
		assertEquals("POST", request.getMethod());
		assertEquals("/async", request.getRequestURI());
	}
    
    @MethodSource("data")
    @ParameterizedTest
    public void validateRequestResponse(String jsonEvent) throws Exception {
    	try (ConfigurableApplicationContext context = SpringApplication.run(EmptyApplication.class);) {
    		ServerlessMVC mvc = ServerlessMVC.INSTANCE((ServletWebServerApplicationContext) context);
    		AwsProxyHttpServletResponseWriter responseWriter = new AwsProxyHttpServletResponseWriter();
    		AwsProxyResponse awsResponse = AwsSpringHttpProcessingUtils.processRequest(
					AwsSpringHttpProcessingUtils.generateHttpServletRequest(jsonEvent, null,
							mvc.getServletContext(), mapper), mvc, responseWriter);
    		assertEquals("hello", awsResponse.getBody());
    		assertEquals(200, awsResponse.getStatusCode());
    	}
    	
    }

	/**
	 * These tests are specific to VPC Lattice
	 */
	@Test
	public void validateHttpServletRequestGenerationWithInputStream() {
		ByteArrayInputStream inputStream = new ByteArrayInputStream(VPC_LATTICE_V2_EVENT.getBytes(StandardCharsets.UTF_8));
		ServerlessServletContext servletContext = new ServerlessServletContext();
		HttpServletRequest request = AwsSpringHttpProcessingUtils.generateLatticeV2HttpServletRequest(inputStream, null, servletContext, mapper);
		// spot check some headers
		assertEquals("application/json", request.getHeader("content-type"));
		assertEquals("10.0.0.39", request.getHeader("X-Forwarded-For"));
		assertEquals("POST", request.getMethod());
		assertEquals("/async", request.getRequestURI());
	}

	@Test
	public void validateHttpServletRequestGenerationWithJson() {
		ServerlessServletContext servletContext = new ServerlessServletContext();
		HttpServletRequest request = AwsSpringHttpProcessingUtils.generateLatticeV2HttpServletRequest(VPC_LATTICE_V2_EVENT, null, servletContext, mapper);
		// spot check some headers
		assertEquals("application/json", request.getHeader("content-type"));
		assertEquals("10.0.0.39", request.getHeader("X-Forwarded-For"));
		assertEquals("POST", request.getMethod());
		assertEquals("/async", request.getRequestURI());
	}

	@Test
	public void validateRequestResponse() throws Exception {
		try (ConfigurableApplicationContext context = SpringApplication.run(EmptyApplication.class);) {
			ServerlessMVC mvc = ServerlessMVC.INSTANCE((ServletWebServerApplicationContext) context);
			AwsProxyHttpServletResponseWriter responseWriter = new AwsProxyHttpServletResponseWriter();
			AwsProxyResponse awsResponse = AwsSpringHttpProcessingUtils.processRequest(
					AwsSpringHttpProcessingUtils.generateLatticeV2HttpServletRequest(VPC_LATTICE_V2_EVENT, null,
							mvc.getServletContext(), mapper), mvc, responseWriter);
			assertEquals("hello", awsResponse.getBody());
			assertEquals(200, awsResponse.getStatusCode());
		}

	}

	@EnableAutoConfiguration
    @Configuration
    public static class EmptyApplication {
    	@RestController
        @EnableWebMvc
        public static class MyController {
        	@PostMapping(path = "/async")
        	public String async(@RequestBody String body) {
        		return "hello";
        	}
        }
    	
    	@Bean
    	SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
    		http.csrf((csrf) -> csrf.disable());
    		return http.build();
    	}
    }
}
