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

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
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
import java.net.URLDecoder;
import java.security.Principal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.*;

public class AwsHttpApiV2ProxyHttpServletRequest extends AwsHttpServletRequest {
    private static Logger log = LoggerFactory.getLogger(AwsHttpApiV2ProxyHttpServletRequest.class);

    private HttpApiV2ProxyRequest request;
    private MultiValuedTreeMap<String, String> queryString;
    private Headers headers;
    private ContainerConfig config;
    private SecurityContext securityContext;
    private AwsAsyncContext asyncContext;

    /**
     * Protected constructors for implementing classes. This should be called first with the context received from
     * AWS Lambda
     *
     * @param lambdaContext The Lambda function context. This object is used for utility methods such as log
     */
    public AwsHttpApiV2ProxyHttpServletRequest(HttpApiV2ProxyRequest req, Context lambdaContext, SecurityContext sc, ContainerConfig cfg) {
        super(lambdaContext);
        request = req;
        config = cfg;
        securityContext = sc;
        queryString = parseRawQueryString(request.getRawQueryString());
        headers = headersMapToMultiValue(request.getHeaders());
    }

    public HttpApiV2ProxyRequest getRequest() {
        return request;
    }

    @Override
    public String getAuthType() {
        // TODO
        return null;
    }

    @Override
    public Cookie[] getCookies() {
        if (headers == null || !headers.containsKey(HttpHeaders.COOKIE)) {
            return new Cookie[0];
        }

        return parseCookieHeaderValue(headers.getFirst(HttpHeaders.COOKIE));
    }

    @Override
    public long getDateHeader(String s) {
        if (headers == null) {
            return -1L;
        }
        String dateString = headers.getFirst(s);
        if (dateString == null) {
            return -1L;
        }
        try {
            return Instant.from(ZonedDateTime.parse(dateString, dateFormatter)).toEpochMilli();
        } catch (DateTimeParseException e) {
            log.warn("Invalid date header in request: " + SecurityUtils.crlf(dateString));
            return -1L;
        }
    }

    @Override
    public String getHeader(String s) {
        if (headers == null) {
            return null;
        }
        return headers.getFirst(s);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        if (headers == null || !headers.containsKey(s)) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.get(s));
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        if (headers == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.keySet());
    }

    @Override
    public int getIntHeader(String s) {
        if (headers == null) {
            return -1;
        }
        String headerValue = headers.getFirst(s);
        if (headerValue == null || "".equals(headerValue)) {
            return -1;
        }

        return Integer.parseInt(headerValue);
    }

    @Override
    public String getMethod() {
        return request.getRequestContext().getHttp().getMethod();
    }

    @Override
    public String getPathInfo() {
        String pathInfo = cleanUri(request.getRawPath());
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
        return request.getRawQueryString();
    }

    @Override
    public String getRemoteUser() {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }
        return securityContext.getUserPrincipal().getName();
    }

    @Override
    public boolean isUserInRole(String s) {
        // TODO: Not supported
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        if (securityContext == null) {
            return null;
        }
        return securityContext.getUserPrincipal();
    }

    @Override
    public String getRequestURI() {
        return cleanUri(getContextPath()) + cleanUri(request.getRawPath());
    }

    @Override
    public StringBuffer getRequestURL() {
        return generateRequestURL(request.getRawPath());
    }


    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public Collection<Part> getParts() throws IOException, ServletException {
        return getMultipartFormParametersMap().values();
    }

    @Override
    public Part getPart(String s) throws IOException, ServletException {
        return getMultipartFormParametersMap().get(s);
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        if (headers == null) {
            return config.getDefaultContentCharset();
        }
        return parseCharacterEncoding(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        if (headers == null || !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }
        String currentContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        headers.putSingle(HttpHeaders.CONTENT_TYPE, appendCharacterEncoding(currentContentType, s));
    }

    @Override
    public int getContentLength() {
        String headerValue = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }

    @Override
    public long getContentLengthLong() {
        String headerValue = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }

    @Override
    public String getContentType() {
        if (headers == null) {
            return null;
        }
        return headers.getFirst(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public String getParameter(String s) {
        String queryStringParameter = getFirstQueryParamValue(queryString, s, config.isQueryStringCaseSensitive());
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
        if (queryString == null) {
            return Collections.emptyEnumeration();
        }

        return Collections.enumeration(queryString.keySet());
    }

    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS") // suppressing this as according to the specs we should be returning null here if we can't find params
    public String[] getParameterValues(String s) {
        List<String> values = new ArrayList<>(Arrays.asList(getQueryParamValues(queryString, s, config.isQueryStringCaseSensitive())));

        values.addAll(Arrays.asList(getFormBodyParameterCaseInsensitive(s)));

        if (values.size() == 0) {
            return null;
        } else {
            return values.toArray(new String[0]);
        }
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return generateParameterMap(queryString, config);
    }

    @Override
    public String getProtocol() {
        return request.getRequestContext().getHttp().getProtocol();
    }

    @Override
    public String getScheme() {
        return getSchemeFromHeader(headers);
    }

    @Override
    public String getServerName() {
        // we match the behavior of the v1 proxy request here. Should we?
        String region = System.getenv("AWS_REGION");
        if (region == null) {
            // this is not a critical failure, we just put a static region in the URI
            region = "us-east-1";
        }

        if (headers != null && headers.containsKey(HOST_HEADER_NAME)) {
            String hostHeader = headers.getFirst(HOST_HEADER_NAME);
            if (SecurityUtils.isValidHost(hostHeader, request.getRequestContext().getApiId(), region)) {
                return hostHeader;
            }
        }

        return request.getRequestContext().getDomainName();
    }

    @Override
    public int getServerPort() {
        if (headers == null || !headers.containsKey(PORT_HEADER_NAME)) {
            return 443; // we default to 443 as HTTP APIs can only be HTTPS
        }
        String port = headers.getFirst(PORT_HEADER_NAME);
        if (SecurityUtils.isValidPort(port)) {
            return Integer.parseInt(port);
        }
        return 443; // default port
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (requestInputStream == null) {
            requestInputStream = new AwsServletInputStream(bodyStringToInputStream(request.getBody(), request.isBase64Encoded()));
        }
        return requestInputStream;
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(request.getBody()));
    }

    @Override
    public String getRemoteAddr() {
        if (request.getRequestContext() == null || request.getRequestContext().getHttp() == null || request.getRequestContext().getHttp().getSourceIp() == null) {
            return "127.0.0.1";
        }
        return request.getRequestContext().getHttp().getSourceIp();
    }

    @Override
    public String getRemoteHost() {
        if (headers == null) {
            return null;
        }
        return headers.getFirst(HttpHeaders.HOST);
    }

    @Override
    public Locale getLocale() {
        // Accept-Language: fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5
        List<HeaderValue> values = this.parseHeaderValue(
                headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE), ",", ";"
        );
        if (values.size() == 0) {
            return Locale.getDefault();
        }
        return new Locale(values.get(0).getValue());
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<HeaderValue> values = this.parseHeaderValue(
                headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE), ",", ";"
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
    public AsyncContext startAsync() throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response, containerHandler);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, containerHandler);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
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

    private MultiValuedTreeMap<String, String> parseRawQueryString(String qs) {
        if (qs == null || "".equals(qs.trim())) {
            return new MultiValuedTreeMap<>();
        }

        MultiValuedTreeMap<String, String> qsMap = new MultiValuedTreeMap<>();
        for (String value : qs.split(QUERY_STRING_SEPARATOR)) {
            try {
                if (!value.contains(QUERY_STRING_KEY_VALUE_SEPARATOR)) {
                    qsMap.add(URLDecoder.decode(value, LambdaContainerHandler.getContainerConfig().getUriEncoding()), null);
                    log.warn("Query string parameter with empty value and no =: " + SecurityUtils.crlf(value));
                    continue;
                }

                String[] kv = value.split(QUERY_STRING_KEY_VALUE_SEPARATOR);
                String key = URLDecoder.decode(kv[0], LambdaContainerHandler.getContainerConfig().getUriEncoding());
                String val = kv.length == 2 ? kv[1] : null;
                qsMap.add(key, val);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding in query string key: " + SecurityUtils.crlf(value), e);
            }
        }
        return qsMap;
    }

    private Headers headersMapToMultiValue(Map<String, String> headers) {
        if (headers == null || headers.size() == 0) {
            return new Headers();
        }

        Headers h = new Headers();
        for (Map.Entry<String, String> hkv : headers.entrySet()) {
            // Exceptions for known header values that contain commas
            if (hkv.getKey().equalsIgnoreCase(HttpHeaders.DATE) ||
                            hkv.getKey().equalsIgnoreCase(HttpHeaders.IF_MODIFIED_SINCE) ||
                            hkv.getKey().toLowerCase(Locale.getDefault()).startsWith("accept-")) {
                h.add(hkv.getKey(), hkv.getValue());
                continue;
            }

            for (String value : hkv.getValue().split(",")) {
                h.add(hkv.getKey(), value);
            }
        }
        return h;
    }
}
