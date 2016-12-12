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


import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

import org.apache.commons.fileupload.FileItem;
import org.apache.commons.fileupload.FileUploadException;
import org.apache.commons.fileupload.servlet.ServletFileUpload;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ReadListener;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;
import javax.servlet.http.HttpUpgradeHandler;
import javax.servlet.http.Part;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
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
public class AwsProxyHttpServletRequest
        implements HttpServletRequest {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final String HEADER_KEY_VALUE_SEPARATOR = "=";
    private static final String HEADER_VALUE_SEPARATOR = ";";
    private static final String FORM_DATA_SEPARATOR = "&";
    private static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
    private static final String HEADER_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";

    // We need this to pickup the protocol from the CloudFront header since Lambda doesn't receive this
    // information from anywhere else
    static final String CF_PROTOCOL_HEADER_NAME = "CloudFront-Forwarded-Proto";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private AwsProxyRequest request;
    private Context lamdaContext;
    private SecurityContext securityContext;
    private Map<String, Object> attributes;
    private Map<String, List<String>> urlEncodedFormParameters;
    private Map<String, Part> multipartFormParameters;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsProxyHttpServletRequest(AwsProxyRequest awsProxyRequest, Context lamdaContext, SecurityContext awsSecurityContext) {
        this.request = awsProxyRequest;
        this.lamdaContext = lamdaContext;
        this.securityContext = awsSecurityContext;

        this.attributes = new HashMap<>();
        this.urlEncodedFormParameters = getFormUrlEncodedParametersMap();
        this.multipartFormParameters = getMultipartFormParametersMap();
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
        String[] cookies = cookieHeader.split(HEADER_VALUE_SEPARATOR);
        List<Cookie> output = new ArrayList<>();

        for (String curCookie : cookies) {
            String[] cookieKeyValue = curCookie.split(HEADER_KEY_VALUE_SEPARATOR);
            if (cookieKeyValue.length < 2) {
                continue;
            }
            output.add(new Cookie(cookieKeyValue[0].trim(), cookieKeyValue[1].trim()));
            // TODO: Parse the full cookie
        }
        Cookie[] returnValue = new Cookie[output.size()];
        return output.toArray(returnValue);
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
            e.printStackTrace();
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
        String pathInfo = getServletPath().replace(getContextPath(), "");
        if (!pathInfo.startsWith("/")) {
            pathInfo = "/" + pathInfo;
        }
        return pathInfo;
    }


    @Override
    public String getPathTranslated() {
        // Return null because it is an archive on a remote system
        return null;
    }


    @Override
    public String getContextPath() {
        return request.getResource();
    }


    @Override
    public String getQueryString() {
        return request.getQueryString().isEmpty() ? null : request.getQueryString();
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
    public String getRequestedSessionId() {
        return null;
    }


    @Override
    public String getRequestURI() {
        return request.getPath();
    }


    @Override
    public StringBuffer getRequestURL() {
        String url = "";
        url += getHeaderCaseInsensitive(HttpHeaders.HOST);
        url += "/";
        url += request.getPath();
        return new StringBuffer(url);
    }


    @Override
    public String getServletPath() {
        return request.getPath();
    }


    @Override
    public HttpSession getSession(boolean b) {
        return null;
    }


    @Override
    public HttpSession getSession() {
        return null;
    }


    @Override
    public String changeSessionId() {
        return null;
    }


    @Override
    public boolean isRequestedSessionIdValid() {
        return false;
    }


    @Override
    public boolean isRequestedSessionIdFromCookie() {
        return false;
    }


    @Override
    public boolean isRequestedSessionIdFromURL() {
        return false;
    }


    @Override
    public boolean isRequestedSessionIdFromUrl() {
        return false;
    }


    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        return false;
    }


    @Override
    public void login(String s, String s1)
            throws ServletException {

    }


    @Override
    public void logout()
            throws ServletException {

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
    public Object getAttribute(String s) {
        return attributes.get(s);
    }


    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }


    @Override
    public String getCharacterEncoding() {
        return getHeaderCaseInsensitive(HttpHeaders.ACCEPT_ENCODING);
    }


    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        request.getHeaders().put(HttpHeaders.ACCEPT_ENCODING, s);
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
    public ServletInputStream getInputStream() throws IOException {
        byte[] bodyBytes = request.getBody().getBytes();
        if (request.isBase64Encoded()) {
            bodyBytes = Base64.getDecoder().decode(request.getBody());
        }
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(bodyBytes);
        return new ServletInputStream() {

            private ReadListener listener;

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
                    e.printStackTrace();
                }
            }


            @Override
            public int read() throws IOException {
                int readByte = requestBodyStream.read();
                if (requestBodyStream.available() == 0 && listener != null) {
                    listener.onAllDataRead();
                }
                return readByte;
            }
        };
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
        paramNames.addAll(request.getQueryStringParameters().keySet());
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
            return null;
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

        for (Map.Entry<String, String> entry : request.getQueryStringParameters().entrySet()) {
            if (params.containsKey(entry.getKey())) {
                params.get(entry.getKey()).add(entry.getValue());
            } else {
                List<String> valueList = new ArrayList<>();
                valueList.add(entry.getValue());
                params.put(entry.getKey(), valueList);
            }
        }

        for (Map.Entry<String, List<String>> entry : params.entrySet()) {
            output.put(entry.getKey(), (String[])entry.getValue().toArray());
        }
        return output;
    }


    @Override
    public String getProtocol() {
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
        return "lambda.amazonaws.com";
    }


    @Override
    public int getServerPort() {
        return 0;
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
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }


    @Override
    public void removeAttribute(String s) {
        attributes.remove(s);
    }


    @Override
    public Locale getLocale() {
        String localeHeader = getHeaderCaseInsensitive(HttpHeaders.ACCEPT_LANGUAGE);
        if (localeHeader == null) {
            return Locale.getDefault();
        }
        if (localeHeader.contains(HEADER_VALUE_SEPARATOR)) {
            localeHeader = localeHeader.split(HEADER_VALUE_SEPARATOR)[0].trim();
        }
        return new Locale(localeHeader);
    }


    @Override
    public Enumeration<Locale> getLocales() {
        String localeHeader = getHeaderCaseInsensitive(HttpHeaders.ACCEPT_LANGUAGE);
        List<Locale> locales = new ArrayList<>();
        if (localeHeader == null) {
            locales.add(Locale.getDefault());
        } else {
            if (localeHeader.contains(HEADER_VALUE_SEPARATOR)) {
                for (String locale : localeHeader.split(HEADER_VALUE_SEPARATOR)) {
                    locales.add(new Locale(locale.trim()));
                }
            } else {
                locales.add(new Locale(localeHeader.trim()));
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
        return null;
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
    public String getLocalName() {
        return "lambda.amazonaws.com";
    }


    @Override
    public String getLocalAddr() {
        return null;
    }


    @Override
    public int getLocalPort() {
        return 0;
    }


    @Override
    public ServletContext getServletContext() {
        return AwsProxyServletContext.getInstance(request, lamdaContext);
    }


    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        return null;
    }


    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        return null;
    }


    @Override
    public boolean isAsyncStarted() {
        return false;
    }


    @Override
    public boolean isAsyncSupported() {
        return false;
    }


    @Override
    public AsyncContext getAsyncContext() {
        return null;
    }


    @Override
    public DispatcherType getDispatcherType() {
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
            if (key.toLowerCase().equals(requestHeaderKey.toLowerCase())) {
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
            if (key.toLowerCase().equals(requestParamKey.toLowerCase())) {
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
                return null;
            }
        }


    private Map<String, Part> getMultipartFormParametersMap() {
        if (!ServletFileUpload.isMultipartContent(this)) { // isMultipartContent also checks the content type
            return new HashMap<>();
        }

        Map<String, Part> output = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        ServletFileUpload upload = new ServletFileUpload();
        try {
            List<FileItem> items = upload.parseRequest(this);
            for (FileItem item : items) {
                AwsProxyRequestPart newPart = new AwsProxyRequestPart(item.get());
                newPart.setName(item.getName());
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
            // TODO: Should we swallaw this?
            e.printStackTrace();
        }
        return output;
    }


    private Map<String, List<String>> getFormUrlEncodedParametersMap() {
        String contentType = getContentType();
        if (contentType == null) {
            return new HashMap<>();
        }
        if (!contentType.startsWith(MediaType.APPLICATION_FORM_URLENCODED) || !getMethod().toLowerCase().equals("post")) {
            return new HashMap<>();
        }
        String rawBodyContent;
        try {
            rawBodyContent = URLDecoder.decode(request.getBody(), DEFAULT_CHARACTER_ENCODING);
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            rawBodyContent = request.getBody();
        }

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
            values.add(parameterKeyValue[1]);
            output.put(parameterKeyValue[0], values);
        }

        return output;
    }
}
