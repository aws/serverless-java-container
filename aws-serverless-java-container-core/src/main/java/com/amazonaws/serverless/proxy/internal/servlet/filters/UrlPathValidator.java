/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy.internal.servlet.filters;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.annotation.WebFilter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
 * Simple path validator filter. This is a default implementation to prevent malformed paths from hitting the framework
 * app. This applies to all paths by default
 */
@WebFilter(filterName = "UrlPathValidator", urlPatterns = {"/*"})
public class UrlPathValidator implements Filter {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    public static final int DEFAULT_ERROR_CODE = 404;
    //public static final Pattern PATH_PATTERN = Pattern.compile("^(/[-\\w:@&?=+,.!/~*'%$_;]*)?$");
    public static final String PARAM_INVALID_STATUS_CODE = "invalid_status_code";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private int invalidStatusCode;
    private Logger log = LoggerFactory.getLogger(UrlPathValidator.class);


    //-------------------------------------------------------------
    // Implementation - Filter
    //-------------------------------------------------------------


    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (filterConfig == null) {
            invalidStatusCode = DEFAULT_ERROR_CODE;
            return;
        }
        if (filterConfig.getInitParameter(PARAM_INVALID_STATUS_CODE) != null) {
            String statusCode = filterConfig.getInitParameter(PARAM_INVALID_STATUS_CODE);
            try {
                invalidStatusCode = Integer.parseInt(statusCode);
            } catch (NumberFormatException e) {
                log.error("Could not parse status code from filter config", e);
                invalidStatusCode = DEFAULT_ERROR_CODE;
            }
        }
    }


    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // This filter has to see the path decoded but NOT normalized. getPathInfo resolves dot segments, which
        // would hide a traversal attempt from the checks below, and the raw URI leaves escapes encoded, so
        // "/%2e%2e/x" would not register as "..". The context path is excluded because it contributes slashes
        // without contributing dot segments, which loosens the ratio check further down.
        HttpServletRequest httpRequest = (HttpServletRequest) servletRequest;
        String path = AwsHttpServletRequest.decodePath(stripContextPath(httpRequest));
        if (path == null) {
            setErrorResponse(servletResponse);
            return;
        }

        // switching to this mechanism to avoid ReDOS attacks on the path pattern regex
        try {
            new URI(path);
        } catch (URISyntaxException e) {
            log.error("Invalid uri path in doFilter", e);
            setErrorResponse(servletResponse);
            return;
        }

        // Logic taken from the Apache UrlValidator. I opted not to include Apache lib as a dependency to save space
        // in the final Lambda function package
        // https://github.com/apache/commons-validator/blob/trunk/src/main/java/org/apache/commons/validator/UrlValidator.java
        //
        // This is a heuristic, not a traversal guard. It only rejects a path whose ".." segments are at least as
        // numerous as the segments before them, so it catches traversal above the root but lets a path with ".."
        // deeper inside it through. Do not rely on it to keep dot segments away from filter selection or routing;
        // FilterChainManager handles those itself.
        int slashCount = countStrings("/", path);
        int dot2Count = countStrings("..", path);
        int slash2Count = countStrings("//", path);
        if (dot2Count > 0 && (slashCount - slash2Count - 1) <= dot2Count){
            setErrorResponse(servletResponse);
            return;
        }

        filterChain.doFilter(servletRequest, servletResponse);
    }


    @Override
    public void destroy() {

    }

    /**
     * Returns the status code used in the errors generated by this filter.
     * @return The default status code the filter will use
     */
    public int getInvalidStatusCode() {
        return invalidStatusCode;
    }


    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private void setErrorResponse(ServletResponse resp) {
        ((HttpServletResponse)resp).setStatus(invalidStatusCode);
    }


    private int countStrings(String needle, String haystack) {
        int curIndex = 0;
        int stringCount = 0;

        while (curIndex != -1) {
            curIndex = haystack.indexOf(needle, curIndex);
            if (curIndex > -1) {
                curIndex++;
                stringCount++;
            }
        }
        return stringCount;
    }

    /**
     * The request URI with the context path removed, still encoded. The context path is configured rather than
     * client-supplied, so including it would only dilute the checks applied to the part the client controls.
     *
     * This deliberately does not reuse AwsHttpServletRequest.contextRelativeRequestUri, which falls back to "/" when
     * the URI is missing. Here a missing URI has to stay null so that doFilter rejects the request.
     */
    private static String stripContextPath(HttpServletRequest request) {
        String uri = request.getRequestURI();
        String contextPath = request.getContextPath();
        if (uri != null && contextPath != null && !contextPath.isEmpty() && uri.startsWith(contextPath)) {
            return uri.substring(contextPath.length());
        }
        return uri;
    }

}
