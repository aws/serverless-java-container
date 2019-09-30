package com.amazonaws.serverless.proxy.spring.echoapp;


import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletResponse;

import java.io.IOException;


public class CustomHeaderFilter implements Filter {
    public static final String HEADER_NAME = "X-Filter-Header";
    public static final String HEADER_VALUE = "CustomHeaderFilter";

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {

    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        HttpServletResponse resp = (HttpServletResponse)servletResponse;
        resp.addHeader(HEADER_NAME, HEADER_VALUE);

        filterChain.doFilter(servletRequest, servletResponse);
    }


    @Override
    public void destroy() {

    }
}