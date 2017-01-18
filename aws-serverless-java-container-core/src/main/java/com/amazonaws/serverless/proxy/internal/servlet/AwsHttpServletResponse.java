/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal.servlet;

import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.net.URLEncoder;
import java.text.SimpleDateFormat;
import java.util.*;
import java.util.concurrent.CountDownLatch;

/**
 * Basic implementation of the <code>HttpServletResponse</code> object. This is used by the <code>AwsProxyHttpServletResponseWriter</code>
 * to generate an <code>AwsProxyResponse</code> object. We have an additional <code>getAwsResponseHeaders()</code> method
 * that returns a {@code Map<String, String>} that can be used for our proxy response object.
 */
public class AwsHttpServletResponse
        implements HttpServletResponse {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final String HEADER_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private MultivaluedHashMap<String, String> headers = new MultivaluedHashMap<>();
    private int statusCode;
    private String statusMessage;
    private String responseBody;
    private ByteArrayOutputStream bodyOutputStream = new ByteArrayOutputStream();
    private CountDownLatch writersCountDownLatch;
    private boolean isCommitted = false;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * The constructor for this object receives a <code>CountDownLatch</code> to synchronize the execution of the Lambda
     * function while the response is asynchronously written by the underlying container/application
     * @param latch A latch used to inform the <code>ContainerHandler</code> that we are done receiving the response data
     */
    public AwsHttpServletResponse(CountDownLatch latch) {
        writersCountDownLatch = latch;
    }


    //-------------------------------------------------------------
    // Implementation - HttpServletResponse
    //-------------------------------------------------------------


    @Override
    public void addCookie(Cookie cookie) {
        String cookieData = cookie.getName() + "=" + cookie.getValue();
        if (cookie.getPath() != null) {
            cookieData += "; Path=" + cookie.getPath();
        }
        setHeader(HttpHeaders.SET_COOKIE, cookieData, false);
    }


    @Override
    public boolean containsHeader(String s) {
        return headers.containsKey(s);
    }


    @Override
    public String encodeURL(String s) {
        return URLEncoder.encode(s);
    }


    @Override
    public String encodeRedirectURL(String s) {
        return URLEncoder.encode(s);
    }


    @Override
    public String encodeUrl(String s) {
        return URLEncoder.encode(s);
    }


    @Override
    public String encodeRedirectUrl(String s) {
        return URLEncoder.encode(s);
    }


    @Override
    public void sendError(int i, String s) throws IOException {
        setStatus(i, s);
        flushBuffer();
    }


    @Override
    public void sendError(int i) throws IOException {
        setStatus(i);
        flushBuffer();
    }


    @Override
    public void sendRedirect(String s) throws IOException {
        setStatus(SC_MOVED_PERMANENTLY);
        addHeader(HttpHeaders.LOCATION, s);
        flushBuffer();
    }


    @Override
    public void setDateHeader(String s, long l) {
        SimpleDateFormat sdf = new SimpleDateFormat(HEADER_DATE_FORMAT);
        Date responseDate = new Date();
        responseDate.setTime(l);
        setHeader(s, sdf.format(responseDate), true);
    }


    @Override
    public void addDateHeader(String s, long l) {
        SimpleDateFormat sdf = new SimpleDateFormat(HEADER_DATE_FORMAT);
        Date responseDate = new Date();
        responseDate.setTime(l);
        setHeader(s, sdf.format(responseDate), false);
    }


    @Override
    public void setHeader(String s, String s1) {
        setHeader(s, s1, true);
    }


    @Override
    public void addHeader(String s, String s1) {
        setHeader(s, s1, false);
    }


    @Override
    public void setIntHeader(String s, int i) {
        setHeader(s, "" + i, true);
    }


    @Override
    public void addIntHeader(String s, int i) {
        setHeader(s, "" + i, false);
    }


    @Override
    public void setStatus(int i) {
        statusCode = i;
    }


    @Override
    public void setStatus(int i, String s) {
        statusCode = i;
        statusMessage = s;
    }


    @Override
    public int getStatus() {
        return statusCode;
    }


    @Override
    public String getHeader(String s) {
        return headers.getFirst(s);
    }


    @Override
    public Collection<String> getHeaders(String s) {
        if (headers.get(s) == null) {
            return new ArrayList<>();
        }
        return headers.get(s);
    }


    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }


    //-------------------------------------------------------------
    // Implementation - ServletResponse
    //-------------------------------------------------------------

    @Override
    public String getCharacterEncoding() {
        return headers.getFirst(HttpHeaders.CONTENT_ENCODING);
    }


    @Override
    public String getContentType() {
        return headers.getFirst(HttpHeaders.CONTENT_TYPE);
    }


    @Override
    public ServletOutputStream getOutputStream() throws IOException {
        return new ServletOutputStream() {
            private WriteListener listener;

            @Override
            public boolean isReady() {
                return true;
            }


            @Override
            public void setWriteListener(WriteListener writeListener) {
                if (writeListener != null) {
                    try {
                        writeListener.onWritePossible();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }

                    listener = writeListener;
                }
            }


            @Override
            public void write(int b) throws IOException {
                try {
                    bodyOutputStream.write(b);
                } catch (Exception e) {
                    listener.onError(e);
                }
            }


            @Override
            public void close()
                    throws IOException {
                super.close();
                flushBuffer();
            }
        };
    }


    @Override
    public PrintWriter getWriter() throws IOException {
        return new PrintWriter(bodyOutputStream);
    }


    @Override
    public void setCharacterEncoding(String s) {
        setHeader(HttpHeaders.CONTENT_ENCODING, s, true);
    }


    @Override
    public void setContentLength(int i) {
        setHeader(HttpHeaders.CONTENT_LENGTH, "" + i, true);
    }


    @Override
    public void setContentLengthLong(long l) {
        setHeader(HttpHeaders.CONTENT_LENGTH, "" + l, true);
    }


    @Override
    public void setContentType(String s) {
        setHeader(HttpHeaders.CONTENT_TYPE, s, true);
    }


    @Override
    public void setBufferSize(int i) {
        bodyOutputStream = new ByteArrayOutputStream(i);
    }


    @Override
    public int getBufferSize() {
        return bodyOutputStream.size();
    }


    @Override
    public void flushBuffer() throws IOException {
        responseBody = new String(bodyOutputStream.toByteArray());
        isCommitted = true;
        writersCountDownLatch.countDown();
    }


    @Override
    public void resetBuffer() {
        bodyOutputStream = new ByteArrayOutputStream();
    }


    @Override
    public boolean isCommitted() {
        return isCommitted;
    }


    @Override
    public void reset() {
        headers = new MultivaluedHashMap<>();
        responseBody = null;
        bodyOutputStream = new ByteArrayOutputStream();
    }


    @Override
    public void setLocale(Locale locale) {
        setHeader(HttpHeaders.CONTENT_LANGUAGE, locale.getLanguage(), true);
    }


    @Override
    public Locale getLocale() {
        return new Locale(getHeader(HttpHeaders.CONTENT_LANGUAGE));
    }


    //-------------------------------------------------------------
    // Methods - Package
    //-------------------------------------------------------------

    String getAwsResponseBodyString() {
        return responseBody;
    }

    byte[] getAwsResponseBodyBytes() {
        if (bodyOutputStream != null) {
            return bodyOutputStream.toByteArray();
        }
        return new byte[0];
    }


    Map<String, String> getAwsResponseHeaders() {
        Map<String, String> responseHeaders = new HashMap<>();
        for (String header : getHeaderNames()) {
            responseHeaders.put(header, headers.getFirst(header));
        }

        return responseHeaders;
    }


    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private void setHeader(String key, String value, boolean overwrite) {
        List<String> values = headers.get(key);

        if (values == null || overwrite) {
            values = new ArrayList<>();
        }

        values.add(value);

        headers.put(key, values);
    }
}
