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
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.amazonaws.services.lambda.runtime.Context;

import org.apache.http.message.BasicHeaderValueParser;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpSession;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;


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
    static final String HEADER_QUALIFIER_SEPARATOR = ",";
    static final String FORM_DATA_SEPARATOR = "&";
    static final DateTimeFormatter dateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    static final String ENCODING_VALUE_KEY = "charset";

    // We need this to pickup the protocol from the CloudFront header since Lambda doesn't receive this
    // information from anywhere else
    static final String CF_PROTOCOL_HEADER_NAME = "CloudFront-Forwarded-Proto";
    static final String PROTOCOL_HEADER_NAME = "X-Forwarded-Proto";
    static final String HOST_HEADER_NAME = "Host";
    static final String PORT_HEADER_NAME = "X-Forwarded-Port";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Context lambdaContext;
    private Map<String, Object> attributes;
    private ServletContext servletContext;
    private AwsHttpSession session;
    private String queryString;
    private BasicHeaderValueParser headerParser;

    protected DispatcherType dispatcherType;

    private Logger log = LoggerFactory.getLogger(AwsHttpServletRequest.class);


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Protected constructors for implementing classes. This should be called first with the context received from
     * AWS Lambda
     * @param lambdaContext The Lambda function context. This object is used for utility methods such as log
     */
    AwsHttpServletRequest(Context lambdaContext) {
        this.lambdaContext = lambdaContext;
        attributes = new HashMap<>();
        headerParser = new BasicHeaderValueParser();
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
            AwsProxyRequestContext requestContext = (AwsProxyRequestContext) getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
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
        List<HeaderValue> parsedHeaders = this.parseHeaderValue(headerValue,  ";", ",");

        return parsedHeaders.stream()
                            .filter(e -> e.getKey() != null)
                            .map(e -> new Cookie(SecurityUtils.crlf(e.getKey()), SecurityUtils.crlf(e.getValue())))
                            .toArray(Cookie[]::new);
    }


    /**
     * Given a map of key/values query string parameters from API Gateway, creates a query string as it would have
     * been in the original url.
     * @param parameters A Map&lt;String, String&gt; of query string parameters
     * @param encode Whether the key and values should be URL encoded
     * @param encodeCharset Charset to use for encoding the query string
     * @return The generated query string for the URI
     */
    protected String generateQueryString(MultiValuedTreeMap<String, String> parameters, boolean encode, String encodeCharset)
            throws ServletException {
        if (parameters == null || parameters.size() == 0) {
            return null;
        }
        if (queryString != null) {
            return queryString;
        }

        StringBuilder queryStringBuilder = new StringBuilder();

        try {
            for (String key : parameters.keySet()) {
                for (String val : parameters.get(key)) {
                    queryStringBuilder.append("&");
                    if (encode) {
                        queryStringBuilder.append(URLEncoder.encode(key, encodeCharset));
                    } else {
                        queryStringBuilder.append(key);
                    }
                    queryStringBuilder.append("=");
                    if (val != null) {
                        if (encode) {
                            queryStringBuilder.append(URLEncoder.encode(val, encodeCharset));
                        } else {
                            queryStringBuilder.append(val);
                        }
                    }
                }
            }
        } catch (UnsupportedEncodingException e) {
            throw new ServletException("Invalid charset passed for query string encoding", e);
        }

        queryString = queryStringBuilder.toString();
        queryString = queryString.substring(1); // remove the first & - faster to do it here than adding logic in the Lambda
        return queryString;
    }


    /**
     * Prases a header value using the default value separator "," and qualifier separator ";".
     * @param headerValue The value to be parsed
     * @return A list of SimpleMapEntry objects with all of the possible values for the header.
     */
    protected List<HeaderValue> parseHeaderValue(String headerValue) {
        return parseHeaderValue(headerValue, HEADER_VALUE_SEPARATOR, HEADER_QUALIFIER_SEPARATOR);
    }

    /**
     * Generic method to parse an HTTP header value and split it into a list of key/values for all its components.
     * When the property in the header does not specify a key the key field in the output pair is null and only the value
     * is populated. For example, The header <code>Accept: application/json; application/xml</code> will contain two
     * key value pairs with key null and the value set to application/json and application/xml respectively.
     *
     * @param headerValue The string value for the HTTP header
     * @param valueSeparator The separator to be used for parsing header values
     * @return A list of SimpleMapEntry objects with all of the possible values for the header.
     */
    protected List<HeaderValue> parseHeaderValue(String headerValue, String valueSeparator, String qualifierSeparator) {
        // Accept: text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8
        // Accept-Language: fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5
        // Cookie: name=value; name2=value2; name3=value3
        // X-Custom-Header: YQ==

        List<HeaderValue> values = new ArrayList<>();
        if (headerValue == null) {
            return values;
        }

        for (String v : headerValue.split(valueSeparator)) {
            String curValue = v;
            float curPreference = 1.0f;
            HeaderValue newValue = new HeaderValue();
            newValue.setRawValue(v);

            for (String q : curValue.split(qualifierSeparator)) {

                String[] kv = q.split(HEADER_KEY_VALUE_SEPARATOR, 2);
                String key = null;
                String val = null;
                // no separator, set the value only
                if (kv.length == 1) {
                    val = q.trim();
                }
                // we have a separator
                if (kv.length == 2) {
                    // if the length of the value is 0 we assume that we are looking at a
                    // base64 encoded value with padding so we just set the value. This is because
                    // we assume that empty values in a key/value pair will contain at least a white space
                    if (kv[1].length() == 0) {
                        val = q.trim();
                    }
                    // this was a base64 string with an additional = for padding, set the value only
                    if ("=".equals(kv[1].trim())) {
                        val = q.trim();
                    } else { // it's a proper key/value set both
                        key = kv[0].trim();
                        val = ("".equals(kv[1].trim()) ? null : kv[1].trim());
                    }
                }

                if (newValue.getValue() == null) {
                    newValue.setKey(key);
                    newValue.setValue(val);
                } else {
                    // special case for quality q=
                    if ("q".equals(key)) {
                        curPreference = Float.parseFloat(val);
                    } else {
                        newValue.addAttribute(key, val);
                    }
                }
            }
            newValue.setPriority(curPreference);
            values.add(newValue);
        }

        // sort list by preference
        values.sort((HeaderValue first, HeaderValue second) -> {
            if ((first.getPriority() - second.getPriority()) < .001f) {
                return 0;
            }
            if (first.getPriority() < second.getPriority()) {
                return 1;
            }
            return -1;
        });
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

    /**
     * Class that represents a header value.
     */
    public static class HeaderValue {
        private String key;
        private String value;
        private String rawValue;
        private float priority;
        private Map<String, String> attributes;

        public HeaderValue() {
            attributes = new HashMap<>();
        }


        public String getKey() {
            return key;
        }


        public void setKey(String key) {
            this.key = key;
        }


        public String getValue() {
            return value;
        }


        public void setValue(String value) {
            this.value = value;
        }


        public String getRawValue() {
            return rawValue;
        }


        public void setRawValue(String rawValue) {
            this.rawValue = rawValue;
        }


        public float getPriority() {
            return priority;
        }


        public void setPriority(float priority) {
            this.priority = priority;
        }


        public Map<String, String> getAttributes() {
            return attributes;
        }


        public void setAttributes(Map<String, String> attributes) {
            this.attributes = attributes;
        }

        public void addAttribute(String key, String value) {
            attributes.put(key, value);
        }

        public String getAttribute(String key) {
            return attributes.get(key);
        }
    }
}
