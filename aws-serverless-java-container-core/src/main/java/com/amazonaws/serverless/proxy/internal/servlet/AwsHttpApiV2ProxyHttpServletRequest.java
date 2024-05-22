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

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
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
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class AwsHttpApiV2ProxyHttpServletRequest extends AwsHttpServletRequest {
    private static Logger log = LoggerFactory.getLogger(AwsHttpApiV2ProxyHttpServletRequest.class);

    private HttpApiV2ProxyRequest request;
    private MultiValuedTreeMap<String, String> queryString;
    private Headers headers;
    private ContainerConfig config;
    private AwsAsyncContext asyncContext;

    /**
     * Protected constructors for implementing classes. This should be called first with the context received from
     * AWS Lambda
     *
     * @param lambdaContext The Lambda function context. This object is used for utility methods such as log
     */
    public AwsHttpApiV2ProxyHttpServletRequest(HttpApiV2ProxyRequest req, Context lambdaContext, SecurityContext sc, ContainerConfig cfg) {
        super(lambdaContext, sc);
        request = req;
        config = cfg;
        queryString = parseRawQueryString(request.getRawQueryString());
        headers = headersMapToMultiValue(request.getHeaders());
    }

    public HttpApiV2ProxyRequest getRequest() {
        return request;
    }

    @Override
    public Cookie[] getCookies() {
        Cookie[] rhc;
        if (headers == null || !headers.containsKey(HttpHeaders.COOKIE)) {
            rhc = new Cookie[0];
        } else {
            rhc = parseCookieHeaderValue(headers.getFirst(HttpHeaders.COOKIE));
        }

        Cookie[] rc;
        if (request.getCookies() == null) {
            rc = new Cookie[0];
        } else {
            rc = request.getCookies().stream()
                .map(c -> {
                    int i = c.indexOf('=');
                    if (i == -1) {
                        return null;
                    } else {
                        String k = SecurityUtils.crlf(c.substring(0, i)).trim();
                        String v = SecurityUtils.crlf(c.substring(i+1));
                        return new Cookie(k, v);
                    }
                })
                .filter(c -> c != null)
                .toArray(Cookie[]::new);
        }

        return Stream.concat(Arrays.stream(rhc), Arrays.stream(rc)).toArray(Cookie[]::new);
    }

    @Override
    public long getDateHeader(String s) {
        return getDateHeader(s, headers);
    }

    @Override
    public String getHeader(String s) {
        return getHeader(s, headers);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        return getHeaders(s, headers);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return getHeaderNames(headers);
    }

    @Override
    public int getIntHeader(String s) {
        return getIntHeader(s, headers);
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
    public String getRequestURI() {
        return cleanUri(getContextPath()) + cleanUri(request.getRawPath());
    }

    @Override
    public StringBuffer getRequestURL() {
        return generateRequestURL(request.getRawPath());
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
        setCharacterEncoding(s, headers);
    }

    @Override
    public int getContentLength() {
        return getContentLength(headers);
    }

    @Override
    public long getContentLengthLong() {
        return getContentLengthLong(headers);
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
        return getParameter(queryString, s, config.isQueryStringCaseSensitive());
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

        if (values.isEmpty()) {
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
            if (SecurityUtils.isValidHost(hostHeader, request.getRequestContext().getApiId(), request.getRequestContext().getElb(), region)) {
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
        List<Locale> locales = parseAcceptLanguageHeader(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));
        return locales.size() == 0 ? Locale.getDefault() : locales.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<Locale> locales = parseAcceptLanguageHeader(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));
        return Collections.enumeration(locales);
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
    public AsyncContext startAsync() throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
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

    @Override
    public String getRequestId() {
        return request.getRequestContext().getRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return "";
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
                String val = kv.length == 2 ? kv[1] : "";
                qsMap.add(key, val);
            } catch (UnsupportedEncodingException e) {
                log.error("Unsupported encoding in query string key: " + SecurityUtils.crlf(value), e);
            }
        }
        return qsMap;
    }

    protected static Headers headersMapToMultiValue(Map<String, String> headers) {
        if (headers == null || headers.size() == 0) {
            return new Headers();
        }

        Headers h = new Headers();
        for (Map.Entry<String, String> hkv : headers.entrySet()) {
            // Exceptions for known header values that contain commas
            if (hkv.getKey().equalsIgnoreCase(HttpHeaders.DATE) ||
                            hkv.getKey().equalsIgnoreCase(HttpHeaders.IF_MODIFIED_SINCE) ||
                            hkv.getKey().equalsIgnoreCase(HttpHeaders.USER_AGENT) ||
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
