package com.amazonaws.serverless.proxy.spring.filterauthapp;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * A deny-by-default authorization filter, mapped to the protected path only. It never calls
 * <code>chain.doFilter</code>, so the response tells you unambiguously whether the filter was selected for the
 * request: 403 means it ran, and the protected body coming back means it did not.
 */
public class AdminAuthorizationFilter implements Filter {
    public static final String FORBIDDEN_BODY = "FORBIDDEN";

    private static final AtomicInteger INVOCATIONS = new AtomicInteger(0);

    public static int getInvocations() {
        return INVOCATIONS.get();
    }

    public static void resetInvocations() {
        INVOCATIONS.set(0);
    }

    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
            throws IOException, ServletException {
        INVOCATIONS.incrementAndGet();
        HttpServletResponse httpResponse = (HttpServletResponse) response;
        httpResponse.setStatus(403);
        httpResponse.setContentType("text/plain");
        httpResponse.getWriter().write(FORBIDDEN_BODY);
        // deliberately not calling chain.doFilter - the request stops here
    }
}
