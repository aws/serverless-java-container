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
