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

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.regex.Pattern;

/**
 * Simple path validator filter. This is a default implementation to prevent malformed paths from hitting the framework
 * app. This applies to all paths by default
 */
@WebFilter(filterName = "UrlPathValidator", urlPatterns = {"/*"})
public class UrlPathValidator implements Filter {
    public static final int DEFAULT_ERROR_CODE = 404;
    public static final Pattern PATH_PATTERN = Pattern.compile("^(/[-\\w:@&?=+,.!/~*'%$_;]*)?$");
    private int invalidStatusCode;

    @Override
    public void init(FilterConfig filterConfig) throws ServletException {
        if (filterConfig.getInitParameter("invalid_status_code") != null) {
            String statusCode = filterConfig.getInitParameter("invalid_status_code");
            try {
                invalidStatusCode = Integer.parseInt(statusCode);
            } catch (NumberFormatException e) {
                invalidStatusCode = DEFAULT_ERROR_CODE;
            }
        }
    }

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
        // the getServletPath method of the AwsProxyHttpServletRequest returns the request path
        String path = ((HttpServletRequest)servletRequest).getServletPath();
        if (path == null) {
            setErrorResponse(servletResponse);
            return;
        }

        if (!PATH_PATTERN.matcher(path).matches()) {
            setErrorResponse(servletResponse);
            return;
        }

        // Logic taken from the Apache UrlValidator. I opted not to include Apache lib as a dependency to save space
        // in the final Lambda function package
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
}
