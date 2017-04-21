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
package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.services.lambda.runtime.Context;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Base HttpServletRequest object. This object exposes some utility methods to work with request values such as headers
 * and query string parameters. New implementations of <code>HttpServletRequest</code> can extend this class to reuse
 * the utility methods
 */
public abstract class AwsHttpServletRequest implements HttpServletRequest {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    static final String HEADER_KEY_VALUE_SEPARATOR = "=";
    static final String HEADER_VALUE_SEPARATOR = ";";
    static final String FORM_DATA_SEPARATOR = "&";
    static final String DEFAULT_CHARACTER_ENCODING = "UTF-8";
    static final String HEADER_DATE_FORMAT = "EEE, d MMM yyyy HH:mm:ss z";
    static final String ENCODING_VALUE_KEY = "charset";

    // We need this to pickup the protocol from the CloudFront header since Lambda doesn't receive this
    // information from anywhere else
    static final String CF_PROTOCOL_HEADER_NAME = "CloudFront-Forwarded-Proto";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Context lambdaContext;
    private Map<String, Object> attributes;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Protected constructors for implemnenting classes. This should be called first with the context received from
     * AWS Lambda
     * @param lambdaContext The Lambda function context. This object is used for utility methods such as log
     */
    AwsHttpServletRequest(Context lambdaContext) {
        this.lambdaContext = lambdaContext;
        attributes = new HashMap<>();
    }

    //-------------------------------------------------------------
    // Implementation - HttpServletRequest
    //-------------------------------------------------------------

    @Override
    public String getRequestedSessionId() {
        throw new UnsupportedOperationException();
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
    public String getServerName() {
        return "lambda.amazonaws.com";
    }


    @Override
    public int getServerPort() {
        return 0;
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
        return AwsServletContext.getInstance(lambdaContext);
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
        return DispatcherType.REQUEST;
    }


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    /**
     * Given the Cookie header value, parses it and creates a Cookie object
     * @param headerValue The string value of the HTTP Cookie header
     * @return An array of Cookie objects from the header
     */
    protected Cookie[] parseCookieHeaderValue(String headerValue) {

        List<Map.Entry<String, String>> parsedHeaders = this.parseHeaderValue(headerValue);

        return parsedHeaders.stream()
                            .filter(e -> e.getKey() != null)
                            .map(e -> new Cookie(e.getKey(), e.getValue()))
                            .toArray(Cookie[]::new);
    }

    /**
     * Given a map of key/values query string parameters from API Gateway, creates a query string as it would have
     * been in the original url.
     * @param parameters A Map<String, String> of query string parameters
     * @return The generated query string for the URI
     */
    protected String generateQueryString(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }

        return parameters.keySet().stream()
                .map(key -> {
                    String newKey = key;
                    String newValue = parameters.get(key);
                    try {
                        if (!URLEncoder.encode(newKey, StandardCharsets.UTF_8.name()).equals(newKey)) {
                            newKey = URLEncoder.encode(key, StandardCharsets.UTF_8.name());
                        }

                        if (!URLEncoder.encode(newValue, StandardCharsets.UTF_8.name()).equals(newValue)) {
                            newValue = URLEncoder.encode(newValue, StandardCharsets.UTF_8.name());
                        }
                    } catch (UnsupportedEncodingException e) {
                        lambdaContext.getLogger().log("Could not URLEncode: " + newKey + "\n" + e.getLocalizedMessage());
                        e.printStackTrace();

                    }
                    return newKey + "=" + newValue;
                })
                .collect(Collectors.joining("&"));
    }


    /**
     * Generic method to parse an HTTP header value and split it into a list of key/values for all its components.
     * When the property in the header does not specify a key the key field in the output pair is null and only the value
     * is populated. For example, The header <code>Accept: application/json; application/xml</code> will contain two
     * key value pairs with key null and the value set to application/json and application/xml respectively.
     *
     * @param headerValue The string value for the HTTP header
     * @return A list of SimpleMapEntry objects with all of the possible values for the header.
     */
    protected List<Map.Entry<String, String>> parseHeaderValue(String headerValue) {
        List<Map.Entry<String, String>> values = new ArrayList<>();
        if (headerValue == null) {
            return values;
        }

        for (String kv : headerValue.split(HEADER_VALUE_SEPARATOR)) {
            String[] kvSplit = kv.split(HEADER_KEY_VALUE_SEPARATOR);

            if (kvSplit.length != 2) {
                values.add(new AbstractMap.SimpleEntry<>(null, kv.trim()));
            } else {
                values.add(new AbstractMap.SimpleEntry<>(kvSplit[0].trim(), kvSplit[1].trim()));
            }
        }
        return values;
    }
}
