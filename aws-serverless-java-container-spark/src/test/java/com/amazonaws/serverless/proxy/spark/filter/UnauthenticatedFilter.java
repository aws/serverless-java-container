package com.amazonaws.serverless.proxy.spark.filter;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
