package com.amazonaws.serverless.proxy.internal.servlet;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpSessionContext;

import java.time.Instant;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;


/**
 * This class emulates the behavior of an HTTP session. At the moment a new instance of this class
 * is created for each request/event. In the future, we may define a session id resolver interface
 * allowing clients to store a map of sessions within a Lambda container.
 */
public class AwsHttpSession implements HttpSession {

    public static final int SESSION_DURATION_SEC = 60 * 30;

    private static final Logger log = LoggerFactory.getLogger(AwsHttpSession.class);
    private Map<String, Object> attributes;
    private String id;
    private long creationTime;
    private int maxInactiveInterval;
    private long lastAccessedTime;
    private boolean valid;

    /**
     * @param id A unique session identifier
     */
    public AwsHttpSession(String id) {
        if (null == id) {
            throw new RuntimeException("HTTP session id (from request ID) cannot be null");
        }
        log.debug("Creating session " + id);
        this.id = id;
        attributes = new HashMap<>();
        creationTime = Instant.now().getEpochSecond();
        maxInactiveInterval = SESSION_DURATION_SEC;
        lastAccessedTime = creationTime;
        valid = true;
    }

    @Override
    public long getCreationTime() {
        return creationTime;
    }

    @Override
    public String getId() {
        return id;
    }

    @Override
    public long getLastAccessedTime() {
        return 0;
    }

    @Override
    public ServletContext getServletContext() {
        return null;
    }

    @Override
    public void setMaxInactiveInterval(int interval) {
        maxInactiveInterval = interval;
    }

    @Override
    public int getMaxInactiveInterval() {
        return maxInactiveInterval;
    }

    @Override
    @Deprecated
    public HttpSessionContext getSessionContext() {
        throw new UnsupportedOperationException("Session context is deprecated and no longer supported");
    }

    @Override
    public Object getAttribute(String name) {
        touch();
        return attributes.get(name);
    }

    @Override
    @Deprecated
    public Object getValue(String name) {
        throw new UnsupportedOperationException("Session values are deprecated and not suported");
    }

    @Override
    public Enumeration<String> getAttributeNames() {
        touch();
        return Collections.enumeration(attributes.keySet());
    }

    @Override
    @Deprecated
    public String[] getValueNames() {
        throw new UnsupportedOperationException("Session values are deprecated and not suported");
    }

    @Override
    public void setAttribute(String name, Object value) {
        touch();
        attributes.put(name, value);
    }

    @Override
    @Deprecated
    public void putValue(String name, Object value) {
        throw new UnsupportedOperationException("Session values are deprecated and not suported");
    }

    @Override
    public void removeAttribute(String name) {
        touch();
        attributes.remove(name);
    }

    @Override
    @Deprecated
    public void removeValue(String name) {
        throw new UnsupportedOperationException("Session values are deprecated and not suported");
    }

    @Override
    public void invalidate() {
        valid = false;
        attributes.clear();
    }

    @Override
    public boolean isNew() {
        return lastAccessedTime == creationTime;
    }

    private void touch() {
        lastAccessedTime = Instant.now().getEpochSecond();
    }

    boolean isValid() {
        if (lastAccessedTime - creationTime < maxInactiveInterval) {
            return valid;
        } else {
            return false;
        }
    }
}
