package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import static com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest.cleanUri;
import static com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest.decodeRequestPath;

public class AwsHttpServletRequestHelper {

    static final DateTimeFormatter dateFormatter = DateTimeFormatter.RFC_1123_DATE_TIME;
    private static final String HEADER_KEY_VALUE_SEPARATOR = "=";

    public static Cookie[] getCookies(Map<String, List<String>> headers) {
        if (headers == null) {
            return new Cookie[0];
        }
        String cookieHeader = getFirst(headers, HttpHeaders.COOKIE);
        if (cookieHeader == null) {
            return new Cookie[0];
        }
        return parseCookieHeaderValue(cookieHeader);
    }

    public static long getDateHeader(Map<String, List<String>> headers, String s, Logger log) {
        if (headers == null) {
            return -1L;
        }
        String dateString = getFirst(headers, s);
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

    public static String getHeader(Map<String, List<String>> headers, String caller, String userAgent, String s, List<String> headerValues) {
        List<String> values = headerValues;
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

    public static Enumeration<String> getHeaders(Map<String, List<String>> headers, String s) {
        if (headers == null || headers.get(s) == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.get(s));
    }

    public static Enumeration<String> getHeaderNames(Map<String, List<String>> headers) {
        if (headers == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.keySet());
    }

    public static int getIntHeader(Map<String, List<String>> headers, String s) {
        if (headers == null) {
            return -1;
        }
        String headerValue = getFirst(headers, s);
        if (headerValue == null) {
            return -1;
        }

        return Integer.parseInt(headerValue);
    }

    public static String getPathInfo(String path) {
        String pathInfo = cleanUri(path);
        return decodeRequestPath(pathInfo, LambdaContainerHandler.getContainerConfig());
    }

    public static String getPathTranslated() {
        // Return null because it is an archive on a remote system
        return null;
    }

    public static String getContextPath(ContainerConfig config, String stage, AwsHttpServletRequest servletRequest) {
        return servletRequest.generateContextPath(config, stage);
    }

    public static String getQueryString(Map<String, List<String>> queryStrings, ContainerConfig config, Logger log, AwsHttpServletRequest servletRequest) {
        try {
            return servletRequest.generateQueryString(
                    queryStrings,
                    // ALB does not automatically decode parameters, so we don't want to re-encode them
                    true,
                    config.getUriEncoding());
        } catch (ServletException e) {
            log.error("Could not generate query string", e);
            return null;
        }
    }

    public static String getRemoteUser(SecurityContext securityContext) {
        return securityContext.getUserPrincipal().getName();
    }

    public static Principal getUserPrincipal(SecurityContext securityContext) {
        return securityContext.getUserPrincipal();
    }

    public static String getRequestURI(String path, AwsHttpServletRequest servletRequest) {
        return cleanUri(servletRequest.getContextPath()) + cleanUri(path);
    }

    public static StringBuffer getRequestURL(String path, AwsHttpServletRequest servletRequest) {
        return servletRequest.generateRequestURL(path);
    }

    public static Collection<Part> getParts(AwsHttpServletRequest servletRequest)
            throws IOException, ServletException {
        return servletRequest.getMultipartFormParametersMap().values();
    }

    public static Part getPart(String s, AwsHttpServletRequest servletRequest)
            throws IOException, ServletException {
        return servletRequest.getMultipartFormParametersMap().get(s);
    }

    public static String getCharacterEncoding(Map<String, List<String>> headers, ContainerConfig config, AwsHttpServletRequest servletRequest) {
        if (headers == null) {
            return config.getDefaultContentCharset();
        }
        return servletRequest.parseCharacterEncoding(getFirst(headers, HttpHeaders.CONTENT_TYPE));
    }

    public static int getContentLength(Map<String, List<String>> headers) {
        String headerValue = getFirst(headers, HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }

    public static long getContentLengthLong(Map<String, List<String>> headers) {
        String headerValue = getFirst(headers, HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }

    public static String getContentType(Map<String, List<String>> headers) {
        String contentTypeHeader = getFirst(headers, HttpHeaders.CONTENT_TYPE);
        if (contentTypeHeader == null || "".equals(contentTypeHeader.trim())) {
            return null;
        }

        return contentTypeHeader;
    }

    public static String getParameter(Map<String, List<String>> queryStrings, String s, ContainerConfig config, AwsHttpServletRequest servletRequest) {
        String queryStringParameter = servletRequest.getFirstQueryParamValue(queryStrings, s, config.isQueryStringCaseSensitive());
        if (queryStringParameter != null) {
            return queryStringParameter;
        }

        String[] bodyParams = servletRequest.getFormBodyParameterCaseInsensitive(s);
        if (bodyParams.length == 0) {
            return null;
        } else {
            return bodyParams[0];
        }
    }

    public static Enumeration<String> getParameterNames(Map<String, List<String>> queryStrings, AwsHttpServletRequest servletRequest) {
        Set<String> formParameterNames = servletRequest.getFormUrlEncodedParametersMap().keySet();
        if (queryStrings == null) {
            return Collections.enumeration(formParameterNames);
        }
        return Collections.enumeration(Stream.concat(formParameterNames.stream(),
                queryStrings.keySet().stream()).collect(Collectors.toSet()));
    }

    public static String[] getParameterValues(Map<String, List<String>> queryStrings, String s, ContainerConfig config, AwsHttpServletRequest servletRequest) {
        List<String> values = new ArrayList<>(Arrays.asList(servletRequest.getQueryParamValues(queryStrings, s, config.isQueryStringCaseSensitive())));

        values.addAll(Arrays.asList(servletRequest.getFormBodyParameterCaseInsensitive(s)));

        if (values.size() == 0) {
            return null;
        } else {
            return values.toArray(new String[0]);
        }
    }

    public static Map<String, String[]> getParameterMap(Map<String, List<String>> queryStrings, ContainerConfig config, AwsHttpServletRequest servletRequest) {
        return servletRequest.generateParameterMap(queryStrings, config);
    }

    public static String getScheme(Map<String, List<String>> headers, AwsHttpServletRequest servletRequest) {
        return servletRequest.getSchemeFromHeader(headers);
    }

    public static String getServerName(Map<String, List<String>> headers, String apiId) {
        String region = System.getenv("AWS_REGION");
        if (region == null) {
            // this is not a critical failure, we just put a static region in the URI
            region = "us-east-1";
        }

        if (headers != null && headers.containsKey(AwsHttpServletRequest.HOST_HEADER_NAME)) {
            String hostHeader = getFirst(headers, AwsHttpServletRequest.HOST_HEADER_NAME);
            if (SecurityUtils.isValidHost(hostHeader, apiId, region)) {
                return hostHeader;
            }
        }

        return new StringBuilder().append(apiId)
                .append(".execute-api.")
                .append(region)
                .append(".amazonaws.com").toString();
    }

    public static int getServerPort(Map<String, List<String>> headers) {
        if (headers == null) {
            return 443;
        }
        String port = getFirst(headers, AwsHttpServletRequest.PORT_HEADER_NAME);
        if (SecurityUtils.isValidPort(port)) {
            return Integer.parseInt(port);
        } else {
            return 443; // default port
        }
    }

    public static ServletInputStream getInputStream(ServletInputStream requestInputStream, String body, boolean isBase64Encoded, AwsHttpServletRequest servletRequest) throws IOException {
        if (requestInputStream == null) {
            requestInputStream = new AwsServletInputStream(servletRequest.bodyStringToInputStream(body, isBase64Encoded));
        }
        return requestInputStream;
    }

    public static BufferedReader getReader(String body)
            throws IOException {
        return new BufferedReader(new StringReader(body));
    }

    public static String getRemoteHost(Map<String, List<String>> headers) {
        return getFirst(headers, HttpHeaders.HOST);
    }

    public static Locale getLocale(Map<String, List<String>> headers, AwsHttpServletRequest servletRequest) {
        List<Locale> locales = servletRequest.parseAcceptLanguageHeader(getFirst(headers, HttpHeaders.ACCEPT_LANGUAGE));
        return locales.size() == 0 ? Locale.getDefault() : locales.get(0);
    }

    public static Enumeration<Locale> getLocales(Map<String, List<String>> headers, AwsHttpServletRequest servletRequest) {
        List<Locale> locales = servletRequest.parseAcceptLanguageHeader(getFirst(headers, HttpHeaders.ACCEPT_LANGUAGE));
        return Collections.enumeration(locales);
    }

    public static boolean isSecure(SecurityContext securityContext) {
        return securityContext.isSecure();
    }

    public static RequestDispatcher getRequestDispatcher(String s, AwsHttpServletRequest servletRequest) {
        return servletRequest.getServletContext().getRequestDispatcher(s);
    }

    public static boolean isAsyncStarted(AwsAsyncContext asyncContext) {
        if (asyncContext == null) {
            return false;
        }
        if (asyncContext.isCompleted() || asyncContext.isDispatched()) {
            return false;
        }
        return true;
    }

    public static AsyncContext startAsync(AwsAsyncContext asyncContext, String requestId, Logger log, AwsHttpServletResponse response, AwsLambdaServletContainerHandler containerHandler, AwsHttpServletRequest servletRequest)
            throws IllegalStateException {
        asyncContext = new AwsAsyncContext(servletRequest, response, containerHandler);
        servletRequest.setAttribute(AwsHttpServletRequest.DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(requestId));
        return asyncContext;
    }


    public static AsyncContext startAsync(AwsAsyncContext asyncContext, String requestId, Logger log, ServletRequest servletRequest, ServletResponse servletResponse, AwsLambdaServletContainerHandler containerHandler)
            throws IllegalStateException {
        servletRequest.setAttribute(AwsHttpServletRequest.DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse, containerHandler);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(requestId));
        return asyncContext;
    }

    public static AsyncContext getAsyncContext(AwsAsyncContext asyncContext, String requestId) {
        if (asyncContext == null) {
            throw new IllegalStateException("Request " + SecurityUtils.crlf(requestId)
                    + " is not in asynchronous mode. Call startAsync before attempting to get the async context.");
        }
        return asyncContext;
    }




    protected static String getFirst(Map<String, List<String>> map, String key) {
        List<String> values = map.get(key);
        if (values == null || values.size() == 0) {
            return null;
        }
        return values.get(0);
    }

    protected void putSingle(Map<String, List<String>> map, String key, String value) {
        List<String> values = findKey(map, key);
        values.clear();
        values.add(value);
    }

    protected List<String> findKey(Map<String, List<String>> map, String key) {
        List<String> values = map.get(key);
        if (values == null) {
            values = new ArrayList<>();
            map.put(key, values);
        }
        return values;
    }

    /**
     * Given the Cookie header value, parses it and creates a Cookie object
     * @param headerValue The string value of the HTTP Cookie header
     * @return An array of Cookie objects from the header
     */
    protected static Cookie[] parseCookieHeaderValue(String headerValue) {
        List<AwsHttpServletRequest.HeaderValue> parsedHeaders = parseHeaderValue(headerValue,  ";", ",");

        return parsedHeaders.stream()
                .filter(e -> e.getKey() != null)
                .map(e -> new Cookie(SecurityUtils.crlf(e.getKey()), SecurityUtils.crlf(e.getValue())))
                .toArray(Cookie[]::new);
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
    protected static List<AwsHttpServletRequest.HeaderValue> parseHeaderValue(String headerValue, String valueSeparator, String qualifierSeparator) {
        // Accept: text/html, application/xhtml+xml, application/xml;q=0.9, */*;q=0.8
        // Accept-Language: fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5
        // Cookie: name=value; name2=value2; name3=value3
        // X-Custom-Header: YQ==

        List<AwsHttpServletRequest.HeaderValue> values = new ArrayList<>();
        if (headerValue == null) {
            return values;
        }

        for (String v : headerValue.split(valueSeparator)) {
            String curValue = v;
            float curPreference = 1.0f;
            AwsHttpServletRequest.HeaderValue newValue = new AwsHttpServletRequest.HeaderValue();
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
        values.sort((AwsHttpServletRequest.HeaderValue first, AwsHttpServletRequest.HeaderValue second) -> {
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


}
