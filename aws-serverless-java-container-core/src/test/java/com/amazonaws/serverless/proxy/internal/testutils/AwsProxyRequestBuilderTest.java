package com.amazonaws.serverless.proxy.internal.testutils;

import java.io.IOException;
import java.net.URLEncoder;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import com.amazonaws.serverless.proxy.model.*;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

public class AwsProxyRequestBuilderTest {

	private static final String TEST_KEY = "testkey";
	private static final String TEST_VALUE = "testvalue";
	private static final String TEST_KEY_FOR_ENCODING = "test@key 1";
	private static final String TEST_VALUE_FOR_ENCODING = "test value!!";


	void baseConstructorAsserts(AwsProxyRequest request) {
		assertEquals(0, request.getMultiValueHeaders().size());
		assertEquals(0, request.getHeaders().size());
		assertEquals(0, request.getMultiValueQueryStringParameters().size());
		assertNotNull(request.getRequestContext());
		assertNotNull(request.getRequestContext().getRequestId());
		assertNotNull(request.getRequestContext().getExtendedRequestId());
		assertEquals("test", request.getRequestContext().getStage());
		assertEquals("HTTP/1.1", request.getRequestContext().getProtocol());
		assertNotNull(request.getRequestContext().getRequestTimeEpoch());
		assertNotNull(request.getRequestContext().getIdentity());
		assertEquals("127.0.0.1", request.getRequestContext().getIdentity().getSourceIp());
	}

	@Test
	void constructor_path_httpMethod() {

		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "GET");
		AwsProxyRequest request = builder.build();
		assertEquals("/path", request.getPath());
		assertEquals("GET", request.getHttpMethod());
		baseConstructorAsserts(request);
	}

	@Test
	void constructor_path_nullHttpMethod() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path");
		AwsProxyRequest request = builder.build();
		assertNull(request.getHttpMethod());
		assertEquals("/path", request.getPath());
		baseConstructorAsserts(request);
	}

	@Test
	void constructor_nullPath_nullHttpMethod() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder();
		AwsProxyRequest request = builder.build();
		assertNull(request.getHttpMethod());
		assertNull(request.getPath());
		baseConstructorAsserts(request);
	}

	@Test
	void form_key_value() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		builder.form(TEST_KEY, TEST_VALUE);
		AwsProxyRequest request = builder.build();
		assertEquals(1, request.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).size());
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
		assertNull(request.getHeaders().get(HttpHeaders.CONTENT_TYPE));
		assertNotNull(request.getBody());
		assertEquals(TEST_KEY + "=" + TEST_VALUE, request.getBody());
	}

	@Test
	void form_key_nullKey_nullValue() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		assertThrows(IllegalArgumentException.class, () -> builder.form(null, TEST_VALUE));
		assertThrows(IllegalArgumentException.class, () -> builder.form(TEST_KEY, null));
		assertThrows(IllegalArgumentException.class, () -> builder.form(null, null));
	}

	@Test
	void form_keyEncoded_valueEncoded() throws IOException {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		builder.form(TEST_KEY_FOR_ENCODING, TEST_VALUE_FOR_ENCODING);
		AwsProxyRequest request = builder.build();

		assertEquals(1, request.getMultiValueHeaders().get(HttpHeaders.CONTENT_TYPE).size());
		assertEquals(MediaType.APPLICATION_FORM_URLENCODED, request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
		assertNull(request.getHeaders().get(HttpHeaders.CONTENT_TYPE));
		assertNotNull(request.getBody());
		String expected = URLEncoder.encode(TEST_KEY_FOR_ENCODING, "UTF-8") + "=" + URLEncoder.encode(TEST_VALUE_FOR_ENCODING, "UTF-8");
		assertEquals(expected, request.getBody());
	}

	@Test
	void queryString_key_value() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		builder.queryString(TEST_KEY, TEST_VALUE);
		AwsProxyRequest request = builder.build();

		assertNull(request.getQueryStringParameters());
		assertEquals(1, request.getMultiValueQueryStringParameters().size());
		assertEquals(TEST_KEY, request.getMultiValueQueryStringParameters().keySet().iterator().next());
		assertEquals(TEST_VALUE, request.getMultiValueQueryStringParameters().get(TEST_KEY).get(0));
		assertEquals(TEST_VALUE, request.getMultiValueQueryStringParameters().getFirst(TEST_KEY));
	}

	@Test
	void queryString_keyNotEncoded_valueNotEncoded() {
		// builder should not URL encode key or value for query string
		// in the case of an ALB where values should be encoded, the builder alb() method will handle it
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		builder.queryString(TEST_KEY_FOR_ENCODING, TEST_VALUE_FOR_ENCODING);
		AwsProxyRequest request = builder.build();

		assertNull(request.getQueryStringParameters());
		assertEquals(1, request.getMultiValueQueryStringParameters().size());
		assertEquals(TEST_KEY_FOR_ENCODING, request.getMultiValueQueryStringParameters().keySet().iterator().next());
		assertEquals(TEST_VALUE_FOR_ENCODING, request.getMultiValueQueryStringParameters().get(TEST_KEY_FOR_ENCODING).get(0));
		assertEquals(TEST_VALUE_FOR_ENCODING, request.getMultiValueQueryStringParameters().getFirst(TEST_KEY_FOR_ENCODING));
	}

	@Test
	void queryString_alb_key_value() {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		builder.queryString(TEST_KEY, TEST_VALUE);
		AwsProxyRequest request = builder.alb().build();

		assertNull(request.getQueryStringParameters());
		assertEquals(1, request.getMultiValueQueryStringParameters().size());
		assertEquals(TEST_KEY, request.getMultiValueQueryStringParameters().keySet().iterator().next());
		assertEquals(TEST_VALUE, request.getMultiValueQueryStringParameters().get(TEST_KEY).get(0));
		assertEquals(TEST_VALUE, request.getMultiValueQueryStringParameters().getFirst(TEST_KEY));
	}

	@Test
	void alb_keyEncoded_valueEncoded() throws IOException {
		AwsProxyRequestBuilder builder = new AwsProxyRequestBuilder("/path", "POST");
		MultiValuedTreeMap<String, String> map = new MultiValuedTreeMap<>();
		map.add(TEST_KEY_FOR_ENCODING, TEST_VALUE_FOR_ENCODING);
		builder.multiValueQueryString(map);
		AwsProxyRequest request = builder.alb().build();

		String expectedKey = URLEncoder.encode(TEST_KEY_FOR_ENCODING, "UTF-8");
		String expectedValue = URLEncoder.encode(TEST_VALUE_FOR_ENCODING, "UTF-8");
		assertEquals(1, request.getMultiValueQueryStringParameters().size());
		assertEquals(expectedKey, request.getMultiValueQueryStringParameters().keySet().iterator().next());
		assertEquals(expectedValue, request.getMultiValueQueryStringParameters().get(expectedKey).get(0));
		assertEquals(expectedValue, request.getMultiValueQueryStringParameters().getFirst(expectedKey));
		assertEquals(expectedKey, request.getMultiValueQueryStringParameters().keySet().iterator().next());
		assertEquals(expectedValue, request.getMultiValueQueryStringParameters().get(expectedKey).get(0));
		assertEquals(expectedValue, request.getMultiValueQueryStringParameters().getFirst(expectedKey));
	}

}
