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
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.RequestSource;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the <code>HttpServletRequest</code> interface that supports <code>APIGatewayProxyRequestEvent</code> object.
 * This object is initialized with an <code>APIGatewayProxyRequestEvent</code> event and a <code>SecurityContext</code> generated
 * by an implementation of the <code>SecurityContextWriter</code>.
 */
public class AwsProxyHttpServletRequest extends AwsHttpServletRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private APIGatewayProxyRequestEvent request;
    private SecurityContext securityContext;
    private AwsAsyncContext asyncContext;
    private static Logger log = LoggerFactory.getLogger(AwsProxyHttpServletRequest.class);
    private ContainerConfig config;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsProxyHttpServletRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext) {
        this(awsProxyRequest, lambdaContext, awsSecurityContext, LambdaContainerHandler.getContainerConfig());
    }


    public AwsProxyHttpServletRequest(APIGatewayProxyRequestEvent awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext, ContainerConfig config) {
        super(lambdaContext);
        this.request = awsProxyRequest;
        this.securityContext = awsSecurityContext;
        this.config = config;
    }

    public APIGatewayProxyRequestEvent getAwsProxyRequest() {
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
        String cookieHeader = getFirst(request.getMultiValueHeaders(), HttpHeaders.COOKIE);
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
        String dateString = getFirst(request.getMultiValueHeaders(), s);
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
        String headerValue = getFirst(request.getMultiValueHeaders(), s);
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
                    true, //request.getRequestSource() != RequestSource.ALB, TODO: check
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
        return parseCharacterEncoding(getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE));
    }


    @Override
    public void setCharacterEncoding(String s)
            throws UnsupportedEncodingException {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        String currentContentType = getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null || "".equals(currentContentType)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }

        putSingle(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE, appendCharacterEncoding(currentContentType, s));
    }


    @Override
    public int getContentLength() {
        String headerValue = getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }


    @Override
    public long getContentLengthLong() {
        String headerValue = getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }


    @Override
    public String getContentType() {
        String contentTypeHeader = getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE);
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
        Set<String> formParameterNames = getFormUrlEncodedParametersMap().keySet();
        if (request.getMultiValueQueryStringParameters() == null) {
            return Collections.enumeration(formParameterNames);
        }
        return Collections.enumeration(Stream.concat(formParameterNames.stream(),
                request.getMultiValueQueryStringParameters().keySet().stream()).collect(Collectors.toSet()));
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
        return null;
        //return request.getRequestContext().getProtocol();    // TODO: Not supported in java-events yet.
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
            String hostHeader = getFirst(request.getMultiValueHeaders(), HOST_HEADER_NAME);
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
        String port = getFirst(request.getMultiValueHeaders(), PORT_HEADER_NAME);
        if (SecurityUtils.isValidPort(port)) {
            return Integer.parseInt(port);
        } else {
            return 443; // default port
        }
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (requestInputStream == null) {
            requestInputStream = new AwsServletInputStream(bodyStringToInputStream(request.getBody(), request.getIsBase64Encoded()));
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
        return getFirst(request.getMultiValueHeaders(), HttpHeaders.HOST);
    }


    @Override
    public Locale getLocale() {
        List<Locale> locales = parseAcceptLanguageHeader(getFirst(request.getMultiValueHeaders(), HttpHeaders.ACCEPT_LANGUAGE));
        return locales.size() == 0 ? Locale.getDefault() : locales.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<Locale> locales = parseAcceptLanguageHeader(getFirst(request.getMultiValueHeaders(), HttpHeaders.ACCEPT_LANGUAGE));
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

    @Override
    public String getRequestId() {
        return request.getRequestContext().getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private List<String> getHeaderValues(String key) {
        // special cases for referer and user agent headers
        List<String> values = new ArrayList<>();

        if (request.getRequestSource() == RequestSource.API_GATEWAY) {
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
