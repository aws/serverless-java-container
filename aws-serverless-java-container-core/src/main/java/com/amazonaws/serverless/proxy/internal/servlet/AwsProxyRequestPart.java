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

import javax.servlet.http.Part;
import javax.ws.rs.core.MultivaluedHashMap;
import java.io.ByteArrayInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collection;

public class AwsProxyRequestPart
        implements Part {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String name;
    private String submittedFileName;
    private long size;
    private String contentType;
    private MultivaluedHashMap<String, String> headers;
    private byte[] content;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsProxyRequestPart(byte[] content) {
        this.content = content;
    }


    //-------------------------------------------------------------
    // Implementation - Part
    //-------------------------------------------------------------


    @Override
    public InputStream getInputStream() throws IOException {
        return new ByteArrayInputStream(content);
    }


    @Override
    public String getContentType() {
        return contentType;
    }


    @Override
    public String getName() {
        return name;
    }


    @Override
    public String getSubmittedFileName() {
        return submittedFileName;
    }


    @Override
    public long getSize() {
        return size;
    }


    @Override
    public void write(String s) throws IOException {
        FileOutputStream fos = new FileOutputStream(s);
        try {
            fos.write(content);
        } finally {
            fos.close();
        }
    }


    @Override
    public void delete() throws IOException {

    }


    @Override
    public String getHeader(String s) {
        return headers.getFirst(s);
    }


    @Override
    public Collection<String> getHeaders(String s) {
        return headers.get(s);
    }


    @Override
    public Collection<String> getHeaderNames() {
        return headers.keySet();
    }


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public void addHeader(String key, String value) {
        if (headers == null) {
            headers = new MultivaluedHashMap<>();
        }

        if (headers.containsKey(key)) {
            headers.add(key, value);
        } else {
            headers.putSingle(key, value);
        }
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public void setName(String name) {
        this.name = name;
    }


    public void setSubmittedFileName(String submittedFileName) {
        this.submittedFileName = submittedFileName;
    }


    public void setSize(long size) {
        this.size = size;
    }


    public void setContentType(String contentType) {
        this.contentType = contentType;
    }


    public void setHeaders(MultivaluedHashMap<String, String> headers) {
        this.headers = headers;
    }
}
