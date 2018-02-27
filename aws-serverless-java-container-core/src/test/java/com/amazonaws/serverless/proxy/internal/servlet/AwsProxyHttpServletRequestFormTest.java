package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.util.Random;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class AwsProxyHttpServletRequestFormTest {
    private static final String PART_KEY_1 = "test1";
    private static final String PART_VALUE_1 = "value1";
    private static final String PART_KEY_2 = "test2";
    private static final String PART_VALUE_2 = "value2";
    private static final String FILE_KEY = "file_upload_1";

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
                                                                                  .addBinaryBody(FILE_KEY, FILE_BYTES)
                                                                                  .build();
    private static final String ENCODED_FORM_ENTITY = PART_KEY_1 + "=" + ENCODED_VALUE + "&" + PART_KEY_2 + "=" + PART_VALUE_2;

    @Test
    public void postForm_getParam_getEncodedFullValue() {
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
    public void postForm_getParts_parsing() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                                                   .header(MULTIPART_FORM_DATA.getContentType().getName(), MULTIPART_FORM_DATA.getContentType().getValue())
                                                   //.header(formData.getContentEncoding().getName(), formData.getContentEncoding().getValue())
                                                   .body(IOUtils.toString(MULTIPART_FORM_DATA.getContent()))
                                                   .build();

            HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
            assertNotNull(request.getParts());
            assertEquals(2, request.getParts().size());
            assertEquals(PART_VALUE_1, IOUtils.toString(request.getPart(PART_KEY_1).getInputStream()));
            assertEquals(PART_VALUE_2, IOUtils.toString(request.getPart(PART_KEY_2).getInputStream()));
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }

    @Test
    public void multipart_getParts_binary() {
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
            assertEquals(PART_VALUE_1, IOUtils.toString(request.getPart(PART_KEY_1).getInputStream()));
            assertEquals(PART_VALUE_2, IOUtils.toString(request.getPart(PART_KEY_2).getInputStream()));
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }
}
