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
import com.amazonaws.services.lambda.runtime.Context;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.disk.DiskFileItemFactory;
import org.apache.commons.fileupload.servlet.ServletFileUpload;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
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
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Iterator;
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
    private Map<String, List<String>> urlEncodedFormParameters;
    private Map<String, Part> multipartFormParameters;
    private static Logger log = LoggerFactory.getLogger(AwsProxyHttpServletRequest.class);
    private ContainerConfig config;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsProxyHttpServletRequest(AwsProxyRequest awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext) {
        this(awsProxyRequest, lambdaContext, awsSecurityContext, ContainerConfig.defaultConfig());
    }


    public AwsProxyHttpServletRequest(AwsProxyRequest awsProxyRequest, Context lambdaContext, SecurityContext awsSecurityContext, ContainerConfig config) {
        super(lambdaContext);
        this.request = awsProxyRequest;
        this.securityContext = awsSecurityContext;
        this.config = config;

        this.urlEncodedFormParameters = getFormUrlEncodedParametersMap();
        this.multipartFormParameters = getMultipartFormParametersMap();
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
        String cookieHeader = getHeaderCaseInsensitive(HttpHeaders.COOKIE);
        if (cookieHeader == null) {
            return new Cookie[0];
        }
        return this.parseCookieHeaderValue(cookieHeader);
    }


    @Override
    public long getDateHeader(String s) {
        String dateString = getHeaderCaseInsensitive(HttpHeaders.DATE);
        if (dateString == null) {
            return new Date().getTime();
        }
        SimpleDateFormat dateFormatter = new SimpleDateFormat(HEADER_DATE_FORMAT);
        try {
            return dateFormatter.parse(dateString).getTime();
        } catch (ParseException e) {
            log.error("Could not parse date header", e);
            return new Date().getTime();
        }
    }


    @Override
    public String getHeader(String s) {
        return getHeaderCaseInsensitive(s);
    }


    @Override
    public Enumeration<String> getHeaders(String s) {
        String headerValue = getHeaderCaseInsensitive(s);
        if (headerValue == null) {
            return Collections.enumeration(new ArrayList<String>());
        }
        List<String> valueCollection = new ArrayList<>();
        valueCollection.add(headerValue);
        return Collections.enumeration(valueCollection);
    }


    @Override
    public Enumeration<String> getHeaderNames() {
        if (request.getHeaders() == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(request.getHeaders().keySet());
    }


    @Override
    public int getIntHeader(String s) {
        String headerValue = getHeaderCaseInsensitive(s);
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
        String pathInfo = cleanUri(request.getPath()); //getServletPath().replace(getContextPath(), "");
        return decodeRequestPath(pathInfo, LambdaContainerHandler.getContainerConfig());
    }


    @Override
    public String getPathTranslated() {
        // Return null because it is an archive on a remote system
        return null;
    }


    @Override
    public String getContextPath() {
        if (config.isUseStageAsServletContext()) {
            String contextPath = cleanUri(request.getRequestContext().getStage());
            if (config.getServiceBasePath() != null) {
                contextPath += cleanUri(config.getServiceBasePath());
            }

            return contextPath;
        } else {
            return "" + (config.getServiceBasePath() != null ? cleanUri(config.getServiceBasePath()) : "");
        }
    }


    @Override
    public String getQueryString() {
        return this.generateQueryString(request.getQueryStringParameters());
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
        return multipartFormParameters.values();
    }


    @Override
    public Part getPart(String s)
            throws IOException, ServletException {
        return multipartFormParameters.get(s);
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
        String contentTypeHeader = getHeaderCaseInsensitive(HttpHeaders.CONTENT_TYPE);
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
        String currentContentType = request.getHeaders().get(HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null) {
            request.getHeaders().put(
                    HttpHeaders.CONTENT_TYPE,
                    HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + s);
            return;
        }

        if (currentContentType.contains(HEADER_VALUE_SEPARATOR)) {
            String[] contentTypeValues = currentContentType.split(HEADER_VALUE_SEPARATOR);
            StringBuilder contentType = new StringBuilder(contentTypeValues[0]);

            for (String contentTypeValue : contentTypeValues) {
                String contentTypeString = HEADER_VALUE_SEPARATOR + " " + contentTypeValue;
                if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                    contentTypeString = HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + s;
                }
                contentType.append(contentTypeString);
            }

            request.getHeaders().put(HttpHeaders.CONTENT_TYPE, contentType.toString());
        } else {
            request.getHeaders().put(
                    HttpHeaders.CONTENT_TYPE,
                    currentContentType + HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + s);
        }
    }


    @Override
    public int getContentLength() {
        String headerValue = getHeaderCaseInsensitive(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }


    @Override
    public long getContentLengthLong() {
        String headerValue = getHeaderCaseInsensitive(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }


    @Override
    public String getContentType() {
        return getHeaderCaseInsensitive(HttpHeaders.CONTENT_TYPE);
    }


    @Override
    public ServletInputStream getInputStream()
            throws IOException {
        if (request.getBody() == null) {
            return null;
        }
        byte[] bodyBytes = null;
        if (request.isBase64Encoded()) {
            bodyBytes = Base64.getMimeDecoder().decode(request.getBody());
        } else {
            bodyBytes = request.getBody().getBytes(StandardCharsets.UTF_8);
        }
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(bodyBytes);
        return new AwsServletInputStream(requestBodyStream);
    }


    @Override
    public String getParameter(String s) {
        String queryStringParameter = getQueryStringParameterCaseInsensitive(s);
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
        List<String> paramNames = new ArrayList<>();
        if (request.getQueryStringParameters() != null) {
            paramNames.addAll(request.getQueryStringParameters().keySet());
        }
        paramNames.addAll(urlEncodedFormParameters.keySet());
        return Collections.enumeration(paramNames);
    }


    @Override
    public String[] getParameterValues(String s) {
        List<String> values = new ArrayList<>();
        String queryStringValue = getQueryStringParameterCaseInsensitive(s);
        if (queryStringValue != null) {
            values.add(queryStringValue);
        }

        String[] formBodyValues = getFormBodyParameterCaseInsensitive(s);
        if (formBodyValues != null) {
            values.addAll(Arrays.asList(formBodyValues));
        }

        if (values.size() == 0) {
            return new String[0];
        } else {
            String[] valuesArray = new String[values.size()];
            valuesArray = values.toArray(valuesArray);
            return valuesArray;
        }
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        Map<String, String[]> output = new HashMap<>();

        Map<String, List<String>> params = urlEncodedFormParameters;
        if (params == null) {
            params = new HashMap<>();
        }

        if (request.getQueryStringParameters() != null) {
            for (Map.Entry<String, String> entry : request.getQueryStringParameters().entrySet()) {
                if (params.containsKey(entry.getKey()) && !params.get(entry.getKey()).contains(entry.getValue())) {
                    params.get(entry.getKey()).add(entry.getValue());
                } else {
                    List<String> valueList = new ArrayList<>();
                    valueList.add(entry.getValue());
                    params.put(entry.getKey(), valueList);
                }
            }
        }

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            String[] valuesArray = new String[entry.getValue().size()];
            valuesArray = entry.getValue().toArray(valuesArray);
            output.put(entry.getKey(), valuesArray);
        }
        return output;
    }


    @Override
    public String getProtocol() {
        // TODO: We should have a cloudfront protocol header
        return null;
    }


    @Override
    public String getScheme() {
        String headerValue = getHeaderCaseInsensitive(CF_PROTOCOL_HEADER_NAME);
        if (headerValue == null) {
            return "https";
        }
        return headerValue;
    }


    @Override
    public String getServerName() {
        String name = getHeaderCaseInsensitive(HttpHeaders.HOST);

        if (name == null || name.length() == 0) {
            name = "lambda.amazonaws.com";
        }
        return name;
    }


    @Override
    public BufferedReader getReader()
            throws IOException {
        return new BufferedReader(new StringReader(request.getBody()));
    }


    @Override
    public String getRemoteAddr() {
        return request.getRequestContext().getIdentity().getSourceIp();
    }


    @Override
    public String getRemoteHost() {
        return getHeaderCaseInsensitive(HttpHeaders.HOST);
    }


    @Override
    public Locale getLocale() {
        List<Map.Entry<String, String>> values = this.parseHeaderValue(
                getHeaderCaseInsensitive(HttpHeaders.ACCEPT_LANGUAGE)
        );
        if (values.size() == 0) {
            return Locale.getDefault();
        }
        return new Locale(values.get(0).getValue());
    }


    @Override
    public Enumeration<Locale> getLocales() {
        List<Map.Entry<String, String>> values = this.parseHeaderValue(
                getHeaderCaseInsensitive(HttpHeaders.ACCEPT_LANGUAGE)
        );
        List<Locale> locales = new ArrayList<>();
        if (values.size() == 0) {
            locales.add(Locale.getDefault());
        } else {
            for (Map.Entry<String, String> locale : values) {
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
    public AsyncContext startAsync()
            throws IllegalStateException {
        return null;
    }


    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        return null;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------


    private String getHeaderCaseInsensitive(String key) {
        if (request.getHeaders() == null) {
            return null;
        }
        for (String requestHeaderKey : request.getHeaders().keySet()) {
            if (key.toLowerCase(Locale.ENGLISH).equals(requestHeaderKey.toLowerCase(Locale.ENGLISH))) {
                return request.getHeaders().get(requestHeaderKey);
            }
        }
        return null;
    }


    private String getQueryStringParameterCaseInsensitive(String key) {
        if (request.getQueryStringParameters() == null) {
            return null;
        }

        for (String requestParamKey : request.getQueryStringParameters().keySet()) {
            if (key.toLowerCase(Locale.ENGLISH).equals(requestParamKey.toLowerCase(Locale.ENGLISH))) {
                return request.getQueryStringParameters().get(requestParamKey);
            }
        }
        return null;
    }


    private String[] getFormBodyParameterCaseInsensitive(String key) {
        List<String> values = urlEncodedFormParameters.get(key);
        if (values != null) {
            String[] valuesArray = new String[values.size()];
            valuesArray = values.toArray(valuesArray);
            return valuesArray;
        } else {
            return new String[0];
        }
    }


    @SuppressFBWarnings("FILE_UPLOAD_FILENAME")
    private Map<String, Part> getMultipartFormParametersMap() {
        if (!ServletFileUpload.isMultipartContent(this)) { // isMultipartContent also checks the content type
            return new HashMap<>();
        }

        Map<String, Part> output = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        ServletFileUpload upload = new ServletFileUpload(new DiskFileItemFactory());
        try {
            List<FileItem> items = upload.parseRequest(this);
            for (FileItem item : items) {
                String fileName = SecurityUtils.getValidFilePath(item.getName(), true);
                AwsProxyRequestPart newPart = new AwsProxyRequestPart(item.get());
                newPart.setName(fileName);
                newPart.setSubmittedFileName(item.getFieldName());
                newPart.setContentType(item.getContentType());
                newPart.setSize(item.getSize());

                Iterator<String> headerNamesIterator = item.getHeaders().getHeaderNames();
                while (headerNamesIterator.hasNext()) {
                    String headerName = headerNamesIterator.next();
                    Iterator<String> headerValuesIterator = item.getHeaders().getHeaders(headerName);
                    while (headerValuesIterator.hasNext()) {
                        newPart.addHeader(headerName, headerValuesIterator.next());
                    }
                }

                output.put(item.getFieldName(), newPart);
            }
        } catch (FileUploadException e) {
            log.error("Could not read multipart upload file", e);
        }
        return output;
    }


    private String cleanUri(String uri) {
        String finalUri = uri;

        if (!finalUri.startsWith("/")) {
            finalUri = "/" + finalUri;
        }

        if (finalUri.endsWith(("/"))) {
            finalUri = finalUri.substring(0, finalUri.length() - 1);
        }

        finalUri = finalUri.replaceAll("/+", "/");

        return finalUri;
    }


    private Map<String, List<String>> getFormUrlEncodedParametersMap() {
        String contentType = getContentType();
        if (contentType == null) {
            return new HashMap<>();
        }
        if (!contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED) || !getMethod().toLowerCase(Locale.ENGLISH).equals("post")) {
            return new HashMap<>();
        }

        String rawBodyContent = request.getBody();

        Map<String, List<String>> output = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String parameter : rawBodyContent.split(FORM_DATA_SEPARATOR)) {
            String[] parameterKeyValue = parameter.split(HEADER_KEY_VALUE_SEPARATOR);
            if (parameterKeyValue.length < 2) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (output.containsKey(parameterKeyValue[0])) {
                values = output.get(parameterKeyValue[0]);
            }
            values.add(decodeValueIfEncoded(parameterKeyValue[1]));
            output.put(decodeValueIfEncoded(parameterKeyValue[0]), values);
        }

        return output;
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
            int readByte = bodyStream.read();
            if (bodyStream.available() == 0 && listener != null) {
                listener.onAllDataRead();
            }
            return readByte;
        }

    }
}
