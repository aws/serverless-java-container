package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.ContentType;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.jupiter.api.Test;

import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.io.IOException;
import java.nio.charset.Charset;
import java.util.Base64;
import java.util.Collections;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.*;


public class AwsProxyHttpServletRequestFormTest {
    private static final String PART_KEY_1 = "test1";
    private static final String PART_VALUE_1 = "value1";
    private static final String PART_KEY_2 = "test2";
    private static final String PART_VALUE_2 = "value2";
    private static final String FILE_KEY = "file_upload_1";
    private static final String FILE_NAME = "testImage.jpg";

    private static final String ENCODED_VALUE = "test123a%3D1%262@3";

    private static final HttpEntity MULTIPART_FORM_DATA = MultipartEntityBuilder.create()
                                                                                .addTextBody(PART_KEY_1, PART_VALUE_1)
                                                                                .addTextBody(PART_KEY_2, PART_VALUE_2)
                                                                                .build();
    private static final int FILE_SIZE = 512;
    private static byte[] FILE_BYTES = new byte[FILE_SIZE];
    static {
        new Random().nextBytes(FILE_BYTES);
    }
    private static final HttpEntity MULTIPART_BINARY_DATA = MultipartEntityBuilder.create()
                                                                                  .addTextBody(PART_KEY_1, PART_VALUE_1)
                                                                                  .addTextBody(PART_KEY_2, PART_VALUE_2)
                                                                                  .addBinaryBody(FILE_KEY, FILE_BYTES, ContentType.IMAGE_JPEG, FILE_NAME)
                                                                                  .build();
    private static final String ENCODED_FORM_ENTITY = PART_KEY_1 + "=" + ENCODED_VALUE + "&" + PART_KEY_2 + "=" + PART_VALUE_2;

    @Test
    void postForm_getParam_getEncodedFullValue() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                    .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED)
                    .body(ENCODED_FORM_ENTITY)
                    .build();

            HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
            assertNotNull(request.getParts());
            assertEquals("test123a=1&2@3", request.getParameter(PART_KEY_1));
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void postForm_getParts_parsing() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                    .header(MULTIPART_FORM_DATA.getContentType().getName(), MULTIPART_FORM_DATA.getContentType().getValue())
                    //.header(formData.getContentEncoding().getName(), formData.getContentEncoding().getValue())
                    .body(IOUtils.toString(MULTIPART_FORM_DATA.getContent(), Charset.defaultCharset()))
                    .build();

            HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
            assertNotNull(request.getParts());
            assertEquals(2, request.getParts().size());
            assertEquals(PART_VALUE_1, IOUtils.toString(request.getPart(PART_KEY_1).getInputStream(), Charset.defaultCharset()));
            assertEquals(PART_VALUE_2, IOUtils.toString(request.getPart(PART_KEY_2).getInputStream(), Charset.defaultCharset()));
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void multipart_getParts_binary() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                    .header(MULTIPART_BINARY_DATA.getContentType().getName(), MULTIPART_BINARY_DATA.getContentType().getValue())
                    .header(HttpHeaders.CONTENT_LENGTH, MULTIPART_BINARY_DATA.getContentLength() + "")
                    .binaryBody(MULTIPART_BINARY_DATA.getContent())
                    .build();

            HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
            assertNotNull(request.getParts());
            assertEquals(3, request.getParts().size());
            assertNotNull(request.getPart(FILE_KEY));
            assertEquals(FILE_SIZE, request.getPart(FILE_KEY).getSize());
            assertEquals(FILE_KEY, request.getPart(FILE_KEY).getName());
            assertEquals(FILE_NAME, request.getPart(FILE_KEY).getSubmittedFileName());
            assertEquals(PART_VALUE_1, IOUtils.toString(request.getPart(PART_KEY_1).getInputStream(), Charset.defaultCharset()));
            assertEquals(PART_VALUE_2, IOUtils.toString(request.getPart(PART_KEY_2).getInputStream(), Charset.defaultCharset()));
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }

    @Test
    void postForm_getParamsBase64Encoded_expectAllParams() {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).build();
        proxyRequest.setBody(Base64.getEncoder().encodeToString(ENCODED_FORM_ENTITY.getBytes(Charset.defaultCharset())));
        proxyRequest.setIsBase64Encoded(true);

        HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
        Map<String, String[]> params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertTrue(params.containsKey(PART_KEY_1));
        assertEquals(2, Collections.list(request.getParameterNames()).size());
    }

    /**
     * issue #340
     */
    @Test
    void postForm_emptyParamPresent() {
        AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED).build();
        String body = PART_KEY_1 + "=" + "&" + PART_KEY_2 + "=" + PART_VALUE_2;
        proxyRequest.setBody(body);

        HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
        Map<String, String[]> params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertTrue(params.containsKey(PART_KEY_1));
        assertEquals(2, Collections.list(request.getParameterNames()).size());
    }
}
