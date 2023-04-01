package com.amazonaws.serverless.proxy.spring.echoapp;


import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import java.io.IOException;


public class UnauthenticatedFilter implements Filter {
    public static final String HEADER_NAME = "X-Unauthenticated-Response";
    public static final int RESPONSE_STATUS = 401;

    @Override
    public void init(FilterConfig filterConfig)
            throws ServletException {

    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
            throws IOException, ServletException {
        if (((HttpServletRequest)servletRequest).getHeader(HEADER_NAME) != null) {
            ((HttpServletResponse) servletResponse).setStatus(401);
            return;
        }
        filterChain.doFilter(servletRequest, servletResponse);
    }


    @Override
    public void destroy() {

    }
}
