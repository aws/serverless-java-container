/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.*;
import javax.servlet.http.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;


/**
 * Implementation of the <code>HttpServletRequest</code> interface that supports <code>AwsProxyRequest</code> object.
 * This object is initialized with an <code>AwsProxyRequest</code> event and a <code>SecurityContext</code> generated
 * by an implementation of the <code>SecurityContextWriter</code>.
 */
public class AwsProxyHttpServletRequest extends AwsHttpServletRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private AwsProxyRequest request;
    private SecurityContext securityContext;
    private AwsAsyncContext asyncContext;
    private static Logger log = LoggerFactory.getLogger(AwsProxyHttpServletRequest.class);
    private ContainerConfig config;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsProxyHttpServletRequest(AwsProxyRequest awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext) {
        this(awsProxyRequest, lambdaContext, awsSecurityContext, LambdaContainerHandler.getContainerConfig());
    }


    public AwsProxyHttpServletRequest(AwsProxyRequest awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext, ContainerConfig config) {
        super(lambdaContext);
        this.request = awsProxyRequest;
        this.securityContext = awsSecurityContext;
        this.config = config;
    }

    public AwsProxyRequest getAwsProxyRequest() {
        return this.request;
    }

    //-------------------------------------------------------------
    // Implementation - HttpServletRequest
    //-------------------------------------------------------------


    @Override
    public String getAuthType() {
        return securityContext.getAuthenticationScheme();
    }


    @Override
    public Cookie[] getCookies() {
        if (request.getMultiValueHeaders() == null) {
            return new Cookie[0];
        }
        String cookieHeader = request.getMultiValueHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookieHeader == null) {
            return new Cookie[0];
        }
        return this.parseCookieHeaderValue(cookieHeader);
    }


    @Override
    public long getDateHeader(String s) {
        if (request.getMultiValueHeaders() == null) {
            return -1L;
        }
        String dateString = request.getMultiValueHeaders().getFirst(s);
        if (dateString == null) {
            return -1L;
        }
        try {
            return Instant.from(ZonedDateTime.parse(dateString, dateFormatter)).toEpochMilli();
        } catch (DateTimeParseException e) {
            log.warn("Invalid date header in request" + SecurityUtils.crlf(dateString));
            return -1L;
        }
    }


    @Override
    public String getHeader(String s) {
        List<String> values = getHeaderValues(s);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }


    @Override
    public Enumeration<String> getHeaders(String s) {
        if (request.getMultiValueHeaders() == null || request.getMultiValueHeaders().get(s) == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(request.getMultiValueHeaders().get(s));
    }


    @Override
    public Enumeration<String> getHeaderNames() {
        if (request.getMultiValueHeaders() == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(request.getMultiValueHeaders().keySet());
    }


    @Override
    public int getIntHeader(String s) {
        if (request.getMultiValueHeaders() == null) {
            return -1;
        }
        String headerValue = request.getMultiValueHeaders().getFirst(s);
        if (headerValue == null) {
            return -1;
        }

        return Integer.parseInt(headerValue);
    }


    @Override
    public String getMethod() {
        return request.getHttpMethod();
    }


    @Override
    public String getPathInfo() {
        String pathInfo = cleanUri(request.getPath());
        return decodeRequestPath(pathInfo, LambdaContainerHandler.getContainerConfig());
    }


    @Override
    public String getPathTranslated() {
        // Return null because it is an archive on a remote system
        return null;
    }


    @Override
    public String getContextPath() {
        return generateContextPath(config, request.getRequestContext().getStage());
    }


    @Override
    public String getQueryString() {
        try {
            return this.generateQueryString(
                    request.getMultiValueQueryStringParameters(),
                    // ALB does not automatically decode parameters, so we don't want to re-encode them
                    request.getRequestSource() != AwsProxyRequest.RequestSource.ALB,
                    config.getUriEncoding());
        } catch (ServletException e) {
            log.error("Could not generate query string", e);
            return null;
        }
    }


    @Override
    public String getRemoteUser() {
        return securityContext.getUserPrincipal().getName();
    }


    @Override
    public boolean isUserInRole(String s) {
        // TODO: Not supported?
        return false;
    }


    @Override
    public Principal getUserPrincipal() {
        return securityContext.getUserPrincipal();
    }


    @Override
    public String getRequestURI() {
        return cleanUri(getContextPath()) + cleanUri(request.getPath());
    }


    @Override
    public StringBuffer getRequestURL() {
        return generateRequestURL(request.getPath());
    }


    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public void login(String s, String s1)
            throws ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public void logout()
            throws ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Collection<Part> getParts()
            throws IOException, ServletException {
        return getMultipartFormParametersMap().values();
    }


    @Override
    public Part getPart(String s)
            throws IOException, ServletException {
        return getMultipartFormParametersMap().get(s);
    }


    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass)
            throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------
    // Implementation - ServletRequest
    //-------------------------------------------------------------


    @Override
    public String getCharacterEncoding() {
        if (request.getMultiValueHeaders() == null) {
            return config.getDefaultContentCharset();
        }
        return parseCharacterEncoding(request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }


    @Override
    public void setCharacterEncoding(String s)
            throws UnsupportedEncodingException {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        String currentContentType = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null || "".equals(currentContentType)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }

        request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_TYPE, appendCharacterEncoding(currentContentType, s));
    }


    @Override
    public int getContentLength() {
        String headerValue = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }


    @Override
    public long getContentLengthLong() {
        String headerValue = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }


    @Override
    public String getContentType() {
        String contentTypeHeader = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentTypeHeader == null || "".equals(contentTypeHeader.trim())) {
            return null;
        }

        return contentTypeHeader;
    }

    @Override
    public String getParameter(String s) {
       String queryStringParameter = getFirstQueryParamValue(request.getMultiValueQueryStringParameters(), s, config.isQueryStringCaseSensitive());
        if (queryStringParameter != null) {
            return queryStringParameter;
        }

        String[] bodyParams = getFormBodyParameterCaseInsensitive(s);
        if (bodyParams.length == 0) {
            return null;
        } else {
            return bodyParams[0];
        }
    }


    @Override
    public Enumeration<String> getParameterNames() {
        if (request.getMultiValueQueryStringParameters() == null) {
            return Collections.emptyEnumeration();
        }

        return Collections.enumeration(request.getMultiValueQueryStringParameters().keySet());
    }


    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS") // suppressing this as according to the specs we should be returning null here if we can't find params
    public String[] getParameterValues(String s) {
        List<String> values = new ArrayList<>(Arrays.asList(getQueryParamValues(request.getMultiValueQueryStringParameters(), s, config.isQueryStringCaseSensitive())));

        values.addAll(Arrays.asList(getFormBodyParameterCaseInsensitive(s)));

        if (values.size() == 0) {
            return null;
        } else {
            return values.toArray(new String[0]);
        }
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        return generateParameterMap(request.getMultiValueQueryStringParameters(), config);
    }


    @Override
    public String getProtocol() {
        return request.getRequestContext().getProtocol();
    }


    @Override
    public String getScheme() {
        return getSchemeFromHeader(request.getMultiValueHeaders());
    }

    @Override
    public String getServerName() {
        String region = System.getenv("AWS_REGION");
        if (region == null) {
            // this is not a critical failure, we just put a static region in the URI
            region = "us-east-1";
        }

        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().containsKey(HOST_HEADER_NAME)) {
            String hostHeader = request.getMultiValueHeaders().getFirst(HOST_HEADER_NAME);
            if (SecurityUtils.isValidHost(hostHeader, request.getRequestContext().getApiId(), region)) {
                return hostHeader;
            }
        }

        return new StringBuilder().append(request.getRequestContext().getApiId())
                                                .append(".execute-api.")
                                                .append(region)
                                                .append(".amazonaws.com").toString();
    }

    @Override
    public int getServerPort() {
        if (request.getMultiValueHeaders() == null) {
            return 443;
        }
        String port = request.getMultiValueHeaders().getFirst(PORT_HEADER_NAME);
        if (SecurityUtils.isValidPort(port)) {
            return Integer.parseInt(port);
        } else {
            return 443; // default port
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (requestInputStream == null) {
            requestInputStream = new AwsServletInputStream(bodyStringToInputStream(request.getBody(), request.isBase64Encoded()));
        }
        return requestInputStream;
    }


    @Override
    public BufferedReader getReader()
            throws IOException {
        return new BufferedReader(new StringReader(request.getBody()));
    }


    @Override
    public String getRemoteAddr() {
        if (request.getRequestContext() == null || request.getRequestContext().getIdentity() == null) {
            return "127.0.0.1";
        }
        return request.getRequestContext().getIdentity().getSourceIp();
    }


    @Override
    public String getRemoteHost() {
        return request.getMultiValueHeaders().getFirst(HttpHeaders.HOST);
    }


    @Override
    public Locale getLocale() {
        // Accept-Language: fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5
        List<HeaderValue> values = this.parseHeaderValue(
                request.getMultiValueHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE), ",", ";"
        );
        if (values.size() == 0) {
            return Locale.getDefault();
        }
        return new Locale(values.get(0).getValue());
    }


    @Override
    public Enumeration<Locale> getLocales() {
        List<HeaderValue> values = this.parseHeaderValue(
                request.getMultiValueHeaders().getFirst(HttpHeaders.ACCEPT_LANGUAGE), ",", ";"
        );

        List<Locale> locales = new ArrayList<>();
        if (values.size() == 0) {
            locales.add(Locale.getDefault());
        } else {
            for (HeaderValue locale : values) {
                locales.add(new Locale(locale.getValue()));
            }
        }

        return Collections.enumeration(locales);
    }


    @Override
    public boolean isSecure() {
        return securityContext.isSecure();
    }


    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return getServletContext().getRequestDispatcher(s);
    }


    @Override
    @Deprecated
    public String getRealPath(String s) {
        // we are in an archive on a remote server
        return null;
    }


    @Override
    public int getRemotePort() {
        return 0;
    }


    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public boolean isAsyncStarted() {
        if (asyncContext == null) {
            return false;
        }
        if (asyncContext.isCompleted() || asyncContext.isDispatched()) {
            return false;
        }
        return true;
    }


    @Override
    public AsyncContext startAsync()
            throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response, containerHandler);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }


    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        servletRequest.setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, containerHandler);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }

    @Override
    public AsyncContext getAsyncContext() {
        if (asyncContext == null) {
            throw new IllegalStateException("Request " + SecurityUtils.crlf(request.getRequestContext().getRequestId())
                    + " is not in asynchronous mode. Call startAsync before attempting to get the async context.");
        }
        return asyncContext;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private List<String> getHeaderValues(String key) {
        // special cases for referer and user agent headers
        List<String> values = new ArrayList<>();

        if (request.getRequestSource() == AwsProxyRequest.RequestSource.API_GATEWAY) {
            if ("referer".equals(key.toLowerCase(Locale.ENGLISH))) {
                values.add(request.getRequestContext().getIdentity().getCaller());
                return values;
            }
            if ("user-agent".equals(key.toLowerCase(Locale.ENGLISH))) {
                values.add(request.getRequestContext().getIdentity().getUserAgent());
                return values;
            }
        }

        if (request.getMultiValueHeaders() == null) {
            return null;
        }

        return request.getMultiValueHeaders().get(key);
    }


}
