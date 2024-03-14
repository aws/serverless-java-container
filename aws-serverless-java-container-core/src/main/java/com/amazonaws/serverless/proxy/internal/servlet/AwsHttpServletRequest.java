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
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.amazonaws.services.lambda.runtime.Context;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.ws.rs.core.HttpHeaders;
import org.apache.commons.fileupload2.core.DiskFileItem;
import org.apache.commons.fileupload2.core.FileItem;
import org.apache.commons.fileupload2.core.FileUploadException;
import org.apache.commons.fileupload2.core.DiskFileItemFactory;
import org.apache.commons.fileupload2.jakarta.servlet6.JakartaServletFileUpload;
import org.apache.commons.io.FilenameUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.input.NullInputStream;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import jakarta.servlet.*;
import jakarta.servlet.http.*;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;


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
    static final String DISPATCHER_TYPE_ATTRIBUTE = "com.amazonaws.serverless.javacontainer.dispatchertype";
    static final String QUERY_STRING_SEPARATOR = "&";
    static final String QUERY_STRING_KEY_VALUE_SEPARATOR = "=";

    // We need this to pickup the protocol from the CloudFront header since Lambda doesn't receive this
    // information from anywhere else
    static final String CF_PROTOCOL_HEADER_NAME = "CloudFront-Forwarded-Proto";
    static final String PROTOCOL_HEADER_NAME = "X-Forwarded-Proto";
    static final String CLIENT_IP_HEADER_NAME = "X-Forwarded-For";

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
    private Map<String, List<Part>> multipartFormParameters;
    private Map<String, List<String>> urlEncodedFormParameters;

    protected AwsHttpServletResponse response;
    protected AwsLambdaServletContainerHandler containerHandler;
    protected ServletInputStream requestInputStream;


    private static Logger log = LoggerFactory.getLogger(AwsHttpServletRequest.class);


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Protected constructors for implementing classes. This should be called first with the context received from
     * AWS Lambda
     * @param lambdaContext The Lambda function context. This object is used for utility methods such as log
     */
    protected AwsHttpServletRequest(Context lambdaContext) {
        this.lambdaContext = lambdaContext;
        attributes = new HashMap<>();
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.REQUEST);
    }

    public AwsHttpServletResponse getResponse() {
        return response;
    }

    public void setResponse(AwsHttpServletResponse response) {
        this.response = response;
    }

    public void setContainerHandler(AwsLambdaServletContainerHandler containerHandler) {
        this.containerHandler = containerHandler;
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


    /**
     * as per Servlet spec this method creates a session if none exists
     * @return exisiting or new http session
     */
    @Override
    public HttpSession getSession() {
        return getSession(true);
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
    public DispatcherType getDispatcherType() {
        if (getAttribute(DISPATCHER_TYPE_ATTRIBUTE) != null) {
            return (DispatcherType) getAttribute(DISPATCHER_TYPE_ATTRIBUTE);
        }
        return DispatcherType.REQUEST;
    }

    @Override
    public String getServletPath() {
        // we always work on the root path
        return "";
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

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
        if (parameters == null || parameters.isEmpty()) {
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

    protected String generateContextPath(ContainerConfig config, String apiStage) {
        String contextPath = "";
        if (config.isUseStageAsServletContext() && apiStage != null) {
            log.debug("Using stage as context path");
            contextPath = cleanUri(apiStage);
        }
        if (config.getServiceBasePath() != null) {
            contextPath += cleanUri(config.getServiceBasePath());
        }

        return contextPath;
    }

    protected StringBuffer generateRequestURL(String requestPath) {
        String url = "";
        url += getServerName();
        url += cleanUri(getContextPath());
        url += cleanUri(requestPath);

        return new StringBuffer(getScheme() + "://" + url);
    }

    protected String parseCharacterEncoding(String contentTypeHeader) {
        // we only look at content-type because content-encoding should only be used for
        // "binary" requests such as gzip/deflate.
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

    protected String appendCharacterEncoding(String currentContentType, String newEncoding) {
        if (currentContentType == null || currentContentType.trim().isEmpty()) {
            return null;
        }

        if (currentContentType.contains(HEADER_VALUE_SEPARATOR)) {
            String[] contentTypeValues = currentContentType.split(HEADER_VALUE_SEPARATOR);
            StringBuilder contentType = new StringBuilder(contentTypeValues[0]);

            for (int i = 1; i < contentTypeValues.length; i++) {
                String contentTypeValue = contentTypeValues[i];
                String contentTypeString = HEADER_VALUE_SEPARATOR + " " + contentTypeValue;
                if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                    contentTypeString = HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + newEncoding;
                }
                contentType.append(contentTypeString);
            }

            return contentType.toString();
        } else {
            return currentContentType + HEADER_VALUE_SEPARATOR + " " + ENCODING_VALUE_KEY + HEADER_KEY_VALUE_SEPARATOR + newEncoding;
        }
    }

    protected ServletInputStream bodyStringToInputStream(String body, boolean isBase64Encoded) throws IOException {
        if (body == null) {
            return new AwsServletInputStream(new NullInputStream(0, false, false));
        }
        byte[] bodyBytes;
        if (isBase64Encoded) {
            bodyBytes = Base64.getMimeDecoder().decode(body);
        } else {
            String encoding = getCharacterEncoding();
            if (encoding == null) {
                encoding = Charset.defaultCharset().name();
            }
            try {
                bodyBytes = body.getBytes(encoding);
            } catch (Exception e) {
                log.error("Could not read request with character encoding: " + SecurityUtils.crlf(encoding), e);
                bodyBytes = body.getBytes(Charset.defaultCharset());
            }
        }
        ByteArrayInputStream requestBodyStream = new ByteArrayInputStream(bodyBytes);
        return new AwsServletInputStream(requestBodyStream);
    }

    protected String getFirstQueryParamValue(MultiValuedTreeMap<String, String> queryString, String key, boolean isCaseSensitive) {
        if (queryString != null) {
            if (isCaseSensitive) {
                return queryString.getFirst(key);
            }

            for (String k : queryString.keySet()) {
                if (k.toLowerCase(Locale.getDefault()).equals(key.toLowerCase(Locale.getDefault()))) {
                    return queryString.getFirst(k);
                }
            }
        }

        return null;
    }

    protected String[] getFormBodyParameterCaseInsensitive(String key) {
        List<String> values = getFormUrlEncodedParametersMap().get(key);
        if (values != null) {
            String[] valuesArray = new String[values.size()];
            valuesArray = values.toArray(valuesArray);
            return valuesArray;
        } else {
            return new String[0];
        }
    }


    protected Map<String, List<String>> getFormUrlEncodedParametersMap() {
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
            rawBodyContent = IOUtils.toString(getInputStream(), getCharacterEncoding());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }

        urlEncodedFormParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);
        for (String parameter : rawBodyContent.split(FORM_DATA_SEPARATOR)) {
            String[] parameterKeyValue = parameter.split(HEADER_KEY_VALUE_SEPARATOR);
            if (parameterKeyValue.length < 1) {
                continue;
            }
            List<String> values = new ArrayList<>();
            if (urlEncodedFormParameters.containsKey(parameterKeyValue[0])) {
                values = urlEncodedFormParameters.get(parameterKeyValue[0]);
            }
            if (parameterKeyValue.length > 1) {
                values.add(decodeValueIfEncoded(parameterKeyValue[1]));
            }
            urlEncodedFormParameters.put(decodeValueIfEncoded(parameterKeyValue[0]), values);
        }
        Timer.stop("SERVLET_REQUEST_GET_FORM_PARAMS");
        return urlEncodedFormParameters;
    }

    @Override
    public Collection<Part> getParts()
            throws IOException, ServletException {
        List<Part> partList =
                getMultipartFormParametersMap().values().stream()
                        .flatMap(List::stream)
                        .collect(Collectors.toList());
        return partList;
    }

    @Override
    public Part getPart(String s)
            throws IOException, ServletException {
        // In case there's multiple files with the same fieldName, we return the first one in the list
        List<Part> values = getMultipartFormParametersMap().get(s);
        if (Objects.isNull(values)) {
            return null;
        }
        return getMultipartFormParametersMap().get(s).get(0);
    }

    @SuppressFBWarnings({"FILE_UPLOAD_FILENAME", "WEAK_FILENAMEUTILS"})
    protected Map<String, List<Part>> getMultipartFormParametersMap() {
        if (multipartFormParameters != null) {
            return multipartFormParameters;
        }
        if (!JakartaServletFileUpload.isMultipartContent(this)) { // isMultipartContent also checks the content type
            multipartFormParameters = new HashMap<>();
            return multipartFormParameters;
        }
        Timer.start("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
        multipartFormParameters = new TreeMap<>(String.CASE_INSENSITIVE_ORDER);

        JakartaServletFileUpload<DiskFileItem, DiskFileItemFactory> upload =
                new JakartaServletFileUpload<>(DiskFileItemFactory.builder().get());

        try {
            List<DiskFileItem> items = upload.parseRequest(this);
            for (FileItem<DiskFileItem> item : items) {
                String fileName = FilenameUtils.getName(item.getName());
                AwsProxyRequestPart newPart = new AwsProxyRequestPart(item.get());
                newPart.setName(item.getFieldName());
                newPart.setSubmittedFileName(fileName);
                newPart.setContentType(item.getContentType());
                newPart.setSize(item.getSize());
                item.getHeaders().getHeaderNames().forEachRemaining(h -> {
                    newPart.addHeader(h, item.getHeaders().getHeader(h));
                });

                addPart(multipartFormParameters, item.getFieldName(), newPart);
            }
        } catch (FileUploadException e) {
            Timer.stop("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
            log.error("Could not read multipart upload file", e);
        }
        Timer.stop("SERVLET_REQUEST_GET_MULTIPART_PARAMS");
        return multipartFormParameters;
    }
    private void addPart(Map<String, List<Part>> params, String fieldName, Part newPart) {
        List<Part> partList = params.get(fieldName);
        if (Objects.isNull(partList)) {
            partList = new ArrayList<>();
            params.put(fieldName, partList);
        }
        partList.add(newPart);
    }

    protected String[] getQueryParamValues(MultiValuedTreeMap<String, String> qs, String key, boolean isCaseSensitive) {
        List<String> value = getQueryParamValuesAsList(qs, key, isCaseSensitive);
        if (value == null){
            return null;
        }
        return value.toArray(new String[0]);
    }

    protected List<String> getQueryParamValuesAsList(MultiValuedTreeMap<String, String> qs, String key, boolean isCaseSensitive) {
        if (qs != null) {
            if (isCaseSensitive) {
                return qs.get(key);
            }

            for (String k : qs.keySet()) {
                if (k.toLowerCase(Locale.getDefault()).equals(key.toLowerCase(Locale.getDefault()))) {
                    return qs.get(k);
                }
            }
        }

        return Collections.emptyList();
    }

    protected String[] getParameterValues(MultiValuedTreeMap<String, String> qs, String s, boolean isCaseSensitive) {
        List<String> values = new ArrayList<>(Arrays.asList(getQueryParamValues(qs, s, isCaseSensitive)));

        values.addAll(Arrays.asList(getFormBodyParameterCaseInsensitive(s)));

        if (values.isEmpty()) {
            return null;
        } else {
            return values.toArray(new String[0]);
        }
    }

    protected Map<String, String[]> generateParameterMap(MultiValuedTreeMap<String, String> qs, ContainerConfig config) {
        Map<String, String[]> output;

        Map<String, List<String>> formEncodedParams = getFormUrlEncodedParametersMap();

        if (qs == null) {
            // Just transform the List<String> values to String[]
            output = formEncodedParams.entrySet().stream()
                .collect(Collectors.toMap(Map.Entry::getKey, (e) -> e.getValue().toArray(new String[0])));
        } else {
            Map<String, List<String>> queryStringParams;
            if (config.isQueryStringCaseSensitive()) {
                queryStringParams = qs;
            } else {
                // If it's case insensitive, we check the entire map on every parameter
                queryStringParams = qs.entrySet().stream().parallel().collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> getQueryParamValuesAsList(qs, e.getKey(), false)
                    ));
            }

            // Merge formEncodedParams and queryStringParams Maps
            output = Stream.of(formEncodedParams, queryStringParams).flatMap(m -> m.entrySet().stream())
                .collect(
                    Collectors.toMap(
                        Map.Entry::getKey,
                        e -> e.getValue().toArray(new String[0]),
                        // If a parameter is in both Maps, we merge the list of values (and ultimately transform to String[])
                        (formParam, queryParam) -> Stream.of(formParam, queryParam).flatMap(Stream::of).toArray(String[]::new)
                    ));

        }

        return output;
    }

    protected String getSchemeFromHeader(Headers headers) {
        // if we don't have any headers to deduce the value we assume HTTPS - API Gateway's default
        if (headers == null) {
            return "https";
        }
        String cfScheme = headers.getFirst(CF_PROTOCOL_HEADER_NAME);
        if (cfScheme != null && SecurityUtils.isValidScheme(cfScheme)) {
            return cfScheme;
        }
        String gwScheme = headers.getFirst(PROTOCOL_HEADER_NAME);
        if (gwScheme != null && SecurityUtils.isValidScheme(gwScheme)) {
            return gwScheme;
        }
        // https is our default scheme
        return "https";
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

        for (String curValue : headerValue.split(valueSeparator)) {
            float curPreference = 1.0f;
            HeaderValue newValue = new HeaderValue();
            newValue.setRawValue(curValue);

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
                    if (kv[1].isEmpty()) {
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

    protected List<Locale> parseAcceptLanguageHeader(String headerValue) {
        // Accept-Language: fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5
        List<HeaderValue> values = this.parseHeaderValue(
                headerValue, ",", ";"
        );

        List<Locale> locales = new ArrayList<>();
        if (values.isEmpty()) {
            locales.add(Locale.getDefault());
        } else {
            for (HeaderValue locale : values) {
                locales.add(parseLanguageTag(locale.getValue()));
            }
        }

        return locales;
    }

    protected Locale parseLanguageTag(String languageTag) {
        languageTag = languageTag.trim();
        String language;
        String country = "";

        int indexDash = languageTag.indexOf('-');
        if (indexDash > -1) {
            country = languageTag.substring(indexDash + 1).trim();
            language = languageTag.substring(0, indexDash).trim();
        } else {
            language = languageTag;
        }

        return new Locale(language, country);
    }

    static String decodeRequestPath(String requestPath, ContainerConfig config) {
        try {
            return URLDecoder.decode(requestPath, config.getUriEncoding());
        } catch (UnsupportedEncodingException ex) {
            log.error("Could not URL decode the request path, configured encoding not supported: {}", SecurityUtils.encode(config.getUriEncoding()));
            // we do not fail at this.
            return requestPath;
        }

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

    static String decodeValueIfEncoded(String value) {
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



    /**
     * These are helper methods meant to reduce duplicated code in servletRequest classes.
     */

    protected long getDateHeader(String s, Headers headers) {
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

    protected String getHeader(String s, Headers headers) {
        if (headers == null) {
            return null;
        }
        return headers.getFirst(s);
    }

    protected Enumeration<String> getHeaders(String s, Headers headers) {
        if (headers == null || !headers.containsKey(s)) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.get(s));
    }

    protected Enumeration<String> getHeaderNames(Headers headers) {
        if (headers == null) {
            return Collections.emptyEnumeration();
        }
        return Collections.enumeration(headers.keySet());
    }

    protected int getIntHeader(String s, Headers headers) {
        if (headers == null) {
            return -1;
        }
        String headerValue = headers.getFirst(s);
        if (headerValue == null || "".equals(headerValue)) {
            return -1;
        }

        return Integer.parseInt(headerValue);
    }

    protected void setCharacterEncoding(String s, Headers headers) throws UnsupportedEncodingException {
        if (headers == null || !headers.containsKey(HttpHeaders.CONTENT_TYPE)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }
        String currentContentType = headers.getFirst(HttpHeaders.CONTENT_TYPE);
        headers.putSingle(HttpHeaders.CONTENT_TYPE, appendCharacterEncoding(currentContentType, s));
    }

    protected int getContentLength(Headers headers) {
        String headerValue = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Integer.parseInt(headerValue);
    }

    protected long getContentLengthLong(Headers headers) {
        String headerValue = headers.getFirst(HttpHeaders.CONTENT_LENGTH);
        if (headerValue == null) {
            return -1;
        }
        return Long.parseLong(headerValue);
    }

    protected String getParameter(MultiValuedTreeMap<String, String> qs, String s, boolean isCaseSensitive) {
        String queryStringParameter = getFirstQueryParamValue(qs, s, isCaseSensitive);
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
