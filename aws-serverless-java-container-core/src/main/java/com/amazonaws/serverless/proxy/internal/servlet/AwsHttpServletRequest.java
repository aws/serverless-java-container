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

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.services.lambda.runtime.Context;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.AbstractMap;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
    static final DateTimeFormatter dateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    static final String ENCODING_VALUE_KEY = "charset";

    // We need this to pickup the protocol from the CloudFront header since Lambda doesn't receive this
    // information from anywhere else
    static final String CF_PROTOCOL_HEADER_NAME = "CloudFront-Forwarded-Proto";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Context lambdaContext;
    private Map<String, Object> attributes;
    private ServletContext servletContext;
    private AwsHttpSession session;
    private String queryString;

    protected DispatcherType dispatcherType;

    private Logger log = LoggerFactory.getLogger(AwsHttpServletRequest.class);


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
        return null;
    }


    @Override
    public HttpSession getSession(boolean b) {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        if (b && null == this.session) {
            ApiGatewayRequestContext requestContext = (ApiGatewayRequestContext) getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
            this.session = new AwsHttpSession(requestContext.getRequestId());
        }
        return this.session;
    }


    @Override
    public HttpSession getSession() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        return this.session;
    }


    @Override
    public String changeSessionId() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        return null;
    }


    @Override
    public boolean isRequestedSessionIdValid() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        return false;
    }


    @Override
    public boolean isRequestedSessionIdFromCookie() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        return false;
    }


    @Override
    public boolean isRequestedSessionIdFromURL() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
        return false;
    }


    @Override
    @Deprecated
    public boolean isRequestedSessionIdFromUrl() {
        log.debug("Trying to access session. Lambda functions are stateless and should not rely on the session");
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
        return servletContext;
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
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public void setDispatcherType(DispatcherType type) {
        dispatcherType = type;
    }

    public void setServletContext(ServletContext context) {
        servletContext = context;
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
                            .map(e -> new Cookie(SecurityUtils.crlf(e.getKey()), SecurityUtils.crlf(e.getValue())))
                            .toArray(Cookie[]::new);
    }


    /**
     * Given a map of key/values query string parameters from API Gateway, creates a query string as it would have
     * been in the original url.
     * @param parameters A Map&lt;String, String&gt; of query string parameters
     * @return The generated query string for the URI
     */
    protected String generateQueryString(Map<String, String> parameters) {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }
        if (queryString != null) {
            return queryString;
        }

        queryString =  parameters.keySet().stream()
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
                        log.error(SecurityUtils.crlf("Could not URLEncode: " + newKey), e);

                    }
                    return newKey + "=" + newValue;
                })
                .collect(Collectors.joining("&"));
        return queryString;
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

    protected String decodeRequestPath(String requestPath, ContainerConfig config) {
        try {
            return URLDecoder.decode(requestPath, config.getUriEncoding());
        } catch (UnsupportedEncodingException ex) {
            log.error("Could not URL decode the request path, configured encoding not supported: {}", SecurityUtils.encode(config.getUriEncoding()));
            // we do not fail at this.
            return requestPath;
        }

    }
}
