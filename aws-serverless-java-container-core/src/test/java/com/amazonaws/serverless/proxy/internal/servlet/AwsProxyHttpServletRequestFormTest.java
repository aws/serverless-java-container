package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.junit.Test;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;


public class AwsProxyHttpServletRequestFormTest {
    private static final String PART_KEY_1 = "test1";
    private static final String PART_VALUE_1 = "value1";
    private static final String PART_KEY_2 = "test2";
    private static final String PART_VALUE_2 = "value2";

    private static final HttpEntity SIMPLE_FORM_DATA = MultipartEntityBuilder.create()
                                                                     .addTextBody(PART_KEY_1, PART_VALUE_1)
                                                                     .addTextBody(PART_KEY_2, PART_VALUE_2)
                                                                     .build();
    @Test
    public void postForm_getParts_parsing() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "POST")
                                                   .header(SIMPLE_FORM_DATA.getContentType().getName(), SIMPLE_FORM_DATA.getContentType().getValue())
                                                   //.header(formData.getContentEncoding().getName(), formData.getContentEncoding().getValue())
                                                   .body(IOUtils.toString(SIMPLE_FORM_DATA.getContent()))
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
    public void getForm_getParts_exception() {
        try {
            AwsProxyRequest proxyRequest = new AwsProxyRequestBuilder("/form", "GET")
                                                   .header(SIMPLE_FORM_DATA.getContentType().getName(), SIMPLE_FORM_DATA.getContentType().getValue())
                                                   //.header(formData.getContentEncoding().getName(), formData.getContentEncoding().getValue())
                                                   .body(IOUtils.toString(SIMPLE_FORM_DATA.getContent()))
                                                   .build();

            HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);
            assertNotNull(request.getParts());
            assertEquals(0, request.getParts().size());
        } catch (IOException | ServletException e) {
            fail(e.getMessage());
        }
    }

    // TODO: Multipart tests
}
