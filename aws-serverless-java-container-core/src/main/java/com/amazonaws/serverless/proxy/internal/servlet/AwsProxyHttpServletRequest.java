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
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.*;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.TreeMap;


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
    private AsyncContext asyncContext;
    private Map<String, List<String>> urlEncodedFormParameters;
    private Map<String, Part> multipartFormParameters;
    private static Logger log = LoggerFactory.getLogger(AwsProxyHttpServletRequest.class);
    private ContainerConfig config;
    private AwsHttpServletResponse response;
    private AwsLambdaServletContainerHandler containerHandler;

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

    public AwsHttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(AwsHttpServletResponse response) {
        this.response = response;
    }

    public AwsLambdaServletContainerHandler getContainerHandler() {
        return containerHandler;
    }

    public void setContainerHandler(AwsLambdaServletContainerHandler containerHandler) {
        this.containerHandler = containerHandler;
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
        String contextPath = "";
        if (config.isUseStageAsServletContext() && request.getRequestContext().getStage() != null) {
            log.debug("Using stage as context path");
            contextPath = cleanUri(request.getRequestContext().getStage());
        }
        if (config.getServiceBasePath() != null) {
            contextPath += cleanUri(config.getServiceBasePath());
        }

        return contextPath;
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
        String url = "";
        url += getServerName();
        url += cleanUri(getContextPath());
        url += cleanUri(request.getPath());

        return new StringBuffer(getScheme() + "://" + url);
    }


    @Override
    public String getServletPath() {
        // we always work on the root path
        return "";
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
        return null;
    }

    //-------------------------------------------------------------
    // Implementation - ServletRequest
    //-------------------------------------------------------------


    @Override
    public String getCharacterEncoding() {
        // we only look at content-type because content-encoding should only be used for
        // "binary" requests such as gzip/deflate.
        String contentTypeHeader = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (contentTypeHeader == null) {
            return null;
        }

        String[] contentTypeValues = contentTypeHeader.split(HEADER_VALUE_SEPARATOR);
        if (contentTypeValues.length <= 1) {
            return null;
        }

        for (String contentTypeValue : contentTypeValues) {
            if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                String[] encodingValues = contentTypeValue.split(HEADER_KEY_VALUE_SEPARATOR);
                if (encodingValues.length <= 1) {
                    return null;
                }
                return encodingValues[1];
            }
        }
        return null;
    }


    @Override
    public void setCharacterEncoding(String s)
            throws UnsupportedEncodingException {
        String currentContentType = request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null || "".equals(currentContentType)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }

        if (currentContentType.contains(HEADER_VALUE_SEPARATOR)) {
            String[] contentTypeValues = currentContentType.split(HEADER_VALUE_SEPARATOR);
            StringBuilder contentType = new StringBuilder(contentTypeValues[0]);

            for (int i = 1; i < contentTypeValues.length; i++) {
                String contentTypeValue = contentTypeValues[i];
                String contentTypeString = HEADER_VALUE_SEPARATOR + " " + contentTypeValue;
                if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                    contentTypeString = HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + s;
                }
                contentType.append(contentTypeString);
            }

            request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_TYPE, contentType.toString());
        } else {
            request.getMultiValueHeaders().putSingle(
                    HttpHeaders.CONTENT_TYPE,
                    currentContentType + HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + s);
        }
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
    public ServletInputStream getInputStream()
            throws IOException {
        if (request.getBody() == null) {
            return new AwsServletInputStream(new NullInputStream(0, false, false));
        }
        byte[] bodyBytes;
        if (request.isBase64Encoded()) {
            bodyBytes = Base64.getMimeDecoder().decode(request.getBody());
        } else {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = StandardCharsets.ISO_8859_1.name();
            }
            try {
                bodyBytes = request.getBody().getBytes(encoding);
            } catch (Exception e) {
                log.error("Could not read request with character encoding: " + SecurityUtils.crlf(encoding), e);
                bodyBytes = request.getBody().getBytes(StandardCharsets.ISO_8859_1.name());
            }

        }
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(bodyBytes);
        return new AwsServletInputStream(requestBodyStream);
    }


    @Override
    public String getParameter(String s) {
       String queryStringParameter = getFirstQueryParamValue(s, config.isQueryStringCaseSensitive());
        if (queryStringParameter != null) {
            return queryStringParameter;
        }

        String[] bodyParams = getFormBodyParameterCaseInsensitive(s);
        if (bodyParams == null || bodyParams.length == 0) {
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
        List<String> values = new ArrayList<>();
        String queryValue = getFirstQueryParamValue(s, config.isQueryStringCaseSensitive());
        if (queryValue != null) {
            values.add(queryValue);
        }

        String[] formBodyValues = getFormBodyParameterCaseInsensitive(s);
        if (formBodyValues != null) {
            values.addAll(Arrays.asList(formBodyValues));
        }

        if (values.size() == 0) {
            return null;
        } else {
            return values.toArray(new String[0]);
        }
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> output = new HashMap<>();

        Map<String, List<String>> params = getFormUrlEncodedParametersMap();
        params.entrySet().stream().parallel().forEach(e -> {
            output.put(e.getKey(), e.getValue().toArray(new String[0]));
        });

        if (request.getMultiValueQueryStringParameters() != null) {
            request.getMultiValueQueryStringParameters().keySet().stream().parallel().forEach(e -> {
                List<String> newValues = new ArrayList<>();
                if (output.containsKey(e)) {
                    String[] values = output.get(e);
                    newValues.addAll(Arrays.asList(values));
                }
                newValues.addAll(Arrays.asList(getQueryParamValues(e, config.isQueryStringCaseSensitive())));
                output.put(e, newValues.toArray(new String[0]));
            });
        }

        return output;
    }


    @Override
    public String getProtocol() {
        return request.getRequestContext().getProtocol();
    }


    @Override
    public String getScheme() {
        // if we don't have any headers to deduce the value we assume HTTPS - API Gateway's default
        if (request.getMultiValueHeaders() == null) {
            return "https";
        }
        String cfScheme = request.getMultiValueHeaders().getFirst(CF_PROTOCOL_HEADER_NAME);
        if (cfScheme != null && SecurityUtils.isValidScheme(cfScheme)) {
            return cfScheme;
        }
        String gwScheme = request.getMultiValueHeaders().getFirst(PROTOCOL_HEADER_NAME);
        if (gwScheme != null && SecurityUtils.isValidScheme(gwScheme)) {
            return gwScheme;
        }
        // https is our default scheme
        return "https";
    }

    @Override
    public String getServerName() {
        String region = System.getenv("AWS_REGION");
        if (region == null) {
            // this is not a critical failure, we just put a static region in the URI
            region = "us-east-1";
        }

        String hostHeader = request.getMultiValueHeaders().getFirst(HOST_HEADER_NAME);
        if (hostHeader != null && SecurityUtils.isValidHost(hostHeader, request.getRequestContext().getApiId(), region)) {
            return hostHeader;
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
    public AsyncContext startAsync()
            throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response, containerHandler);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }


    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, containerHandler);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getRequestId()));
        return asyncContext;
    }

    @Override
    public AsyncContext getAsyncContext() {
        if (asyncContext == null) {
            throw new IllegalStateException("Request " + SecurityUtils.crlf(request.getRequestContext().getRequestId())
                    + " is not in asynchronous mode. Call startAsync before atttempting to get the async context.");
        }
        return asyncContext;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private String[] getFormBodyParameterCaseInsensitive(String key) {
        List<String> values = getFormUrlEncodedParametersMap().get(key);
        if (values != null) {
            String[] valuesArray = new String[values.size()];
            valuesArray = values.toArray(valuesArray);
            return valuesArray;
        } else {
            return new String[0];
        }
    }


    @SuppressFBWarnings({"FILE_UPLOAD_FILENAME", "WEAK_FILENAMEUTILS"})
    private Map<String, Part> getMultipartFormParametersMap() {
        if (multipartFormParameters != null) {
            return multipartFormParameters;
        }
        if (!ServletFileUpload.isMultipartContent(this)) { // isMultipartContent also checks the content type
            multipartFormParameters = new HashMap<>();
            return multipartFormParameters;
        }
        Timer.start("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
        multipartFormParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());

        try {
            List<FileItem> items = upload.parseRequest(this);
            for (FileItem item : items) {
                String fileName = FilenameUtils.getName(item.getName());
                AwsProxyRequestPart newPart = new AwsProxyRequestPart(item.get());
                newPart.setName(item.getFieldName());
                newPart.setSubmittedFileName(fileName);
                newPart.setContentType(item.getContentType());
                newPart.setSize(item.getSize());
                item.getHeaders().getHeaderNames().forEachRemaining(h -> {
                    newPart.addHeader(h, item.getHeaders().getHeader(h));
                });

                multipartFormParameters.put(item.getFieldName(), newPart);
            }
        } catch (FileUploadException e) {
            Timer.stop("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
            log.error("Could not read multipart upload file", e);
        }
        Timer.stop("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
        return multipartFormParameters;
    }


    static String cleanUri(String uri) {
        String finalUri = (uri == null ? "/" : uri);
        if (finalUri.equals("/")) {
            return finalUri;
        }

        if (!finalUri.startsWith("/")) {
            finalUri = "/" + finalUri;
        }

        if (finalUri.endsWith("/")) {
            finalUri = finalUri.substring(0, finalUri.length() - 1);
        }

        finalUri = finalUri.replaceAll("/+", "/");

        return finalUri;
    }


    private Map<String, List<String>> getFormUrlEncodedParametersMap() {
        if (urlEncodedFormParameters != null) {
            return urlEncodedFormParameters;
        }
        String contentType = getContentType();
        if (contentType == null) {
            urlEncodedFormParameters = new HashMap<>();
            return urlEncodedFormParameters;
        }
        if (!contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED) || !getMethod().toLowerCase(Locale.ENGLISH).equals("post")) {
            urlEncodedFormParameters = new HashMap<>();
            return urlEncodedFormParameters;
        }
        Timer.start("SERVLET_REQUEST_GET_FORM_PARAMS");
        String rawBodyContent = null;
        try {
            rawBodyContent = IOUtils.toString(getInputStream());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        urlEncodedFormParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String parameter : rawBodyContent.split(FORM_DATA_SEPARATOR)) {
            String[] parameterKeyValue = parameter.split(HEADER_KEY_VALUE_SEPARATOR);
            if (parameterKeyValue.length < 2) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (urlEncodedFormParameters.containsKey(parameterKeyValue[0])) {
                values = urlEncodedFormParameters.get(parameterKeyValue[0]);
            }
            values.add(decodeValueIfEncoded(parameterKeyValue[1]));
            urlEncodedFormParameters.put(decodeValueIfEncoded(parameterKeyValue[0]), values);
        }
        Timer.stop("SERVLET_REQUEST_GET_FORM_PARAMS");
        return urlEncodedFormParameters;
    }


    public static String decodeValueIfEncoded(String value) {
        if (value == null) {
            return null;
        }

        try {
            return URLDecoder.decode(value, LambdaContainerHandler.getContainerConfig().getUriEncoding());
        } catch (UnsupportedEncodingException e) {
            log.warn("Could not decode body content - proceeding as if it was already decoded", e);
            return value;
        }
    }

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


    private String getFirstQueryParamValue(String key, boolean isCaseSensitive) {
        if (request.getMultiValueQueryStringParameters() != null) {
            if (isCaseSensitive) {
                return request.getMultiValueQueryStringParameters().getFirst(key);
            }
            
            for (String k : request.getMultiValueQueryStringParameters().keySet()) {
                if (k.toLowerCase(Locale.getDefault()).equals(key.toLowerCase(Locale.getDefault()))) {
                    return request.getMultiValueQueryStringParameters().getFirst(k);
                }
            }
        }

        return null;
    }

    public String[] getQueryParamValues(String key, boolean isCaseSensitive) {
        if (request.getMultiValueQueryStringParameters() != null) {
            if (isCaseSensitive) {
                return request.getMultiValueQueryStringParameters().get(key).toArray(new String[0]);
            }

            for (String k : request.getMultiValueQueryStringParameters().keySet()) {
                if (k.toLowerCase(Locale.getDefault()).equals(key.toLowerCase(Locale.getDefault()))) {
                    return request.getMultiValueQueryStringParameters().get(k).toArray(new String[0]);
                }
            }
        }

        return new String[0];
    }


    public static class AwsServletInputStream extends ServletInputStream {

        private InputStream bodyStream;
        private ReadListener listener;

        public AwsServletInputStream(InputStream body) {
            bodyStream = body;
        }


        @Override
        public boolean isFinished() {
            return true;
        }


        @Override
        public boolean isReady() {
            return true;
        }


        @Override
        public void setReadListener(ReadListener readListener) {
            listener = readListener;
            try {
                listener.onDataAvailable();
            } catch (IOException e) {
                log.error("Data not available on input stream", e);
            }
        }


        @Override
        public int read()
                throws IOException {
            if (bodyStream == null || bodyStream instanceof NullInputStream) {
                return -1;
            }
            int readByte = bodyStream.read();
            if (bodyStream.available() == 0 && listener != null) {
                listener.onAllDataRead();
            }
            return readByte;
        }

    }
}
