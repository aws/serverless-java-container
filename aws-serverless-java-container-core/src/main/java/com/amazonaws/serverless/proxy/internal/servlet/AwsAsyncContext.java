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

import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;

/**
 * Async context for Serverless Java Container. This is used to support reactive embedded servers for our support for
 * Spring Boot 2. Behind the scenes, the Async context still uses the <code>CountDownLatch</code> to synchronize response
 * generation.
 */
public class AwsAsyncContext implements AsyncContext {
    private HttpServletRequest req;
    private HttpServletResponse res;
    private AwsLambdaServletContainerHandler handler;
    private List<AsyncListenerHolder> listeners;
    private long timeout;
    private AtomicBoolean dispatched;
    private AtomicBoolean completed;

    private Logger log = LoggerFactory.getLogger(AwsAsyncContext.class);

    public AwsAsyncContext(HttpServletRequest request, HttpServletResponse response, AwsLambdaServletContainerHandler servletHandler) {
        log.debug("Initializing async context for request: " + SecurityUtils.crlf(request.getPathInfo()) + " - " + SecurityUtils.crlf(request.getMethod()));
        req = request;
        res = response;
        handler = servletHandler;
        listeners = new ArrayList<>();
        timeout = 3000;
        dispatched = new AtomicBoolean(false);
        completed = new AtomicBoolean(false);
    }

    @Override
    public ServletRequest getRequest() {
        return req;
    }

    @Override
    public ServletResponse getResponse() {
        return res;
    }

    @Override
    public boolean hasOriginalRequestAndResponse() {
        return true;
    }

    @Override
    public void dispatch() {
        try {
            log.debug("Dispatching request");
            if (dispatched.get()) {
                throw new IllegalStateException("Dispatching already started");
            }
            dispatched.set(true);
            notifyListeners(NotificationType.START_ASYNC, null);
            handler.doFilter(req, res, ((AwsServletContext)req.getServletContext()).getServletForPath(req.getRequestURI()));
        } catch (ServletException | IOException e) {
            notifyListeners(NotificationType.ERROR, e);
        }
    }

    @Override
    public void dispatch(String s) {
        // amend the request path
        req = new AwsHttpServletRequestWrapper(req, s);

        dispatch();
    }

    @Override
    public void dispatch(ServletContext servletContext, String s) {
        req = new AwsHttpServletRequestWrapper(req, s);
        ((AwsHttpServletRequestWrapper)req).setServletContext(servletContext);
        dispatch(s);
    }

    @Override
    public void complete() {
        try {
            log.debug("Completing request");
            notifyListeners(NotificationType.COMPLETE, null);
            res.flushBuffer();
            completed.set(true);
        } catch (IOException e) {
            log.error("Could not flush response buffer", e);
            throw new RuntimeException(e);
        }
    }

    @Override
    public void start(Runnable runnable) {
        throw new UnsupportedOperationException("Operation not supported");
    }

    @Override
    public void addListener(AsyncListener asyncListener) {
        listeners.add(new AsyncListenerHolder(asyncListener, this));
    }

    @Override
    public void addListener(AsyncListener asyncListener, ServletRequest servletRequest, ServletResponse servletResponse) {
        AsyncListenerHolder holder = new AsyncListenerHolder(asyncListener, this);
        holder.setSuppliedRequest(servletRequest);
        holder.setSuppliedResponse(servletResponse);
        listeners.add(holder);
    }

    @Override
    public <T extends AsyncListener> T createListener(Class<T> aClass) throws ServletException {
        try {
            return aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            throw new ServletException(e);
        }
    }

    @Override
    public void setTimeout(long l) {
        timeout = l;
    }

    @Override
    public long getTimeout() {
        return timeout;
    }

    public boolean isDispatched() {
        return dispatched.get();
    }

    public boolean isCompleted() {
        return completed.get();
    }

    private void notifyListeners(NotificationType type, Throwable t) {
        listeners.forEach((h) -> {
            try {
                switch (type) {
                    case COMPLETE:
                    case START_ASYNC:
                    case TIMEOUT:
                        h.getListener().onComplete(h.getAsyncEvent());
                        break;
                    case ERROR:
                        h.getListener().onError(h.getAsyncEvent(t));
                        break;
                }
            } catch (IOException e) {
                if (type != NotificationType.ERROR) {
                    notifyListeners(NotificationType.ERROR, e);
                }
            }
        });
    }

    private enum NotificationType {
        COMPLETE,
        ERROR,
        START_ASYNC,
        TIMEOUT
    }

    /**
     * The listener holder wraps and <code>AsyncListener</code> with information about its context such as the request,
     * response, and async servlet context.
     */
    private static final class AsyncListenerHolder {
        private AsyncListener listener;
        private ServletRequest suppliedRequest;
        private ServletResponse suppliedResponse;
        private AsyncContext context;

        public AsyncListenerHolder(AsyncListener l, AsyncContext ctx) {
            listener = l;
            context = ctx;
        }

        public AsyncListener getListener() {
            return listener;
        }

        public void setListener(AsyncListener listener) {
            this.listener = listener;
        }

        public ServletRequest getSuppliedRequest() {
            return suppliedRequest;
        }

        public void setSuppliedRequest(ServletRequest suppliedRequest) {
            this.suppliedRequest = suppliedRequest;
        }

        public ServletResponse getSuppliedResponse() {
            return suppliedResponse;
        }

        public void setSuppliedResponse(ServletResponse suppliedResponse) {
            this.suppliedResponse = suppliedResponse;
        }

        public AsyncEvent getAsyncEvent(Throwable t) {
            if (suppliedRequest != null && suppliedResponse != null) {
                if (t != null) {
                    return new AsyncEvent(context, suppliedRequest, suppliedResponse, t);
                }
                return new AsyncEvent(context, suppliedRequest, suppliedResponse);
            }
            return new AsyncEvent(context);
        }

        public AsyncEvent getAsyncEvent() {
            return getAsyncEvent(null);
        }
    }
}
