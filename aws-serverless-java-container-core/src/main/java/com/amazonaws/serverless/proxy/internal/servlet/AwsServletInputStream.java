/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import org.apache.commons.io.input.NullInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import java.io.IOException;
import java.io.InputStream;

public class AwsServletInputStream extends ServletInputStream {
    private static Logger log = LoggerFactory.getLogger(AwsServletInputStream.class);
    private InputStream bodyStream;
    private ReadListener listener;
    private boolean finished;

    public AwsServletInputStream(InputStream body) {
        bodyStream = body;
        finished = false;
    }

    @Override
    public boolean isFinished() {
        return finished;
    }

    @Override
    public boolean isReady() {
        if (finished && listener != null) {
            try {
                listener.onAllDataRead();
            } catch (IOException e) {
                log.error("Could not notify listeners that input stream data is ready", e);
                throw new RuntimeException(e);
            }
        }
        return !finished;
    }

    @Override
    public void setReadListener(ReadListener readListener) {
        listener = readListener;
        try {
            listener.onDataAvailable();
        } catch (IOException e) {
            log.error("Could not notify listeners that data is available", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public int read()
            throws IOException {
        if (bodyStream == null || bodyStream instanceof NullInputStream) {
            return -1;
        }
        int readByte = bodyStream.read();
        if (readByte == -1) {
            finished = true;
        }
        return readByte;
    }
}
