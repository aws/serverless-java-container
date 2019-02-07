package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Enumeration;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static org.junit.Assert.*;

public class AwsProxyHttpServletRequestTest {
    private static final String CUSTOM_HEADER_KEY = "X-Custom-Header";
    private static final String CUSTOM_HEADER_VALUE = "Custom-Header-Value";
    private static final String FORM_PARAM_NAME = "name";
    private static final String FORM_PARAM_NAME_VALUE = "Stef";
    private static final String FORM_PARAM_TEST = "test_cookie_param";
    private static final String QUERY_STRING_NAME_VALUE = "Bob";
    private static final String REQUEST_SCHEME_HTTP = "http";
    private static final String USER_AGENT = "Mozilla/5.0 (Android 4.4; Mobile; rv:41.0) Gecko/41.0 Firefox/41.0";
    private static final String REFERER = "https://developer.mozilla.org/en-US/docs/Web/HTTP/Headers/User-Agent/Firefox";
    private static ZonedDateTime REQUEST_DATE = ZonedDateTime.now();

    private static final AwsProxyRequest REQUEST_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_INVALID_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "GET")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_FORM_URLENCODED_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_SINGLE_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MULTIPLE_COOKIES = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .cookie(FORM_PARAM_TEST, FORM_PARAM_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MALFORMED_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.COOKIE, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_MULTIPLE_FORM_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_TEST, QUERY_STRING_NAME_VALUE).build();
    private static final AwsProxyRequest REQUEST_USER_AGENT_REFERER = new AwsProxyRequestBuilder("/hello", "POST")
            .userAgent(USER_AGENT)
            .referer(REFERER).build();
    private static final AwsProxyRequest REQUEST_WITH_DATE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.DATE, AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE))
            .build();
    private static final AwsProxyRequest REQUEST_WITH_LOWERCASE_HEADER = new AwsProxyRequestBuilder("/hello", "POST")
            .header(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault()), MediaType.APPLICATION_JSON).build();

    private static final AwsProxyRequest REQUEST_NULL_QUERY_STRING;
    static {
        AwsProxyRequest awsProxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        awsProxyRequest.setMultiValueQueryStringParameters(null);
        REQUEST_NULL_QUERY_STRING = awsProxyRequest;
    }

    private static final AwsProxyRequest REQUEST_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE).build();


    @Test
    public void headers_getHeader_validRequest() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
    }

    @Test
    public void headers_getRefererAndUserAgent_returnsContextValues() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_USER_AGENT_REFERER, null, null);
        assertNotNull(request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("referer"));

        assertNotNull(request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("user-agent"));
    }

    @Test
    public void formParams_getParameter_validForm() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getParameter(FORM_PARAM_NAME));
        assertEquals(FORM_PARAM_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_INVALID_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNull(request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_multipleParams() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
    }

    @Test
    public void formParams_getParameter_queryStringPrecendence() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void dateHeader_noDate_returnNegativeOne() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(-1L, request.getDateHeader(HttpHeaders.DATE));
    }

    @Test
    public void dateHeader_correctDate_parseToCorrectLong() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_WITH_DATE, null, null);
        assertNotNull(request);

        String instantString = AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE);
        assertEquals(Instant.from(AwsHttpServletRequest.dateFormatter.parse(instantString)).toEpochMilli(), request.getDateHeader(HttpHeaders.DATE));
        assertEquals(-1L, request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
    }

    @Test
    public void scheme_getScheme_https() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals("https", request.getScheme());
    }

    @Test
    public void scheme_getScheme_http() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals(REQUEST_SCHEME_HTTP, request.getScheme());
    }

    @Test
    public void cookie_getCookies_noCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void cookie_getCookies_singleCookie() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_SINGLE_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(1, request.getCookies().length);
        assertEquals(FORM_PARAM_NAME, request.getCookies()[0].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[0].getValue());
    }

    @Test
    public void cookie_getCookies_multipleCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MULTIPLE_COOKIES, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(2, request.getCookies().length);
        assertEquals(FORM_PARAM_NAME, request.getCookies()[0].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[0].getValue());
        assertEquals(FORM_PARAM_TEST, request.getCookies()[1].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[1].getValue());
    }

    @Test
    public void cookie_getCookies_emptyCookies() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MALFORMED_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void queryParameters_getParameterMap_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_NULL_QUERY_STRING, null, null);
        assertNotNull(request);
        assertEquals(0, request.getParameterMap().size());
    }

    @Test
    public void queryParameters_getParameterMap_nonNull() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_QUERY, null, null);
        assertNotNull(request);
        assertEquals(1, request.getParameterMap().size());
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameterMap().get(FORM_PARAM_NAME)[0]);
    }

    @Test
    public void queryParameters_getParameterNames_null() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_NULL_QUERY_STRING, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(0, parameterNames.size());
    }

    @Test
    public void queryParameters_getParameterNames_notNull() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_QUERY, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(1, parameterNames.size());
        assertTrue(parameterNames.contains(FORM_PARAM_NAME));
    }

    @Test
    public void queryParameter_getParameterMap_avoidDuplicationOnMultipleCalls() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_MULTIPLE_FORM_AND_QUERY, null, null);

        Map<String, String[]> params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertNotNull(params.get(FORM_PARAM_NAME));
        assertEquals(1, params.get(FORM_PARAM_NAME).length);
        assertNotNull(params.get(FORM_PARAM_TEST));
        assertEquals(1, params.get(FORM_PARAM_TEST).length);

        params = request.getParameterMap();
        assertNotNull(params);
        assertEquals(2, params.size());
        assertNotNull(params.get(FORM_PARAM_NAME));
        assertEquals(1, params.get(FORM_PARAM_NAME).length);
        assertNotNull(params.get(FORM_PARAM_TEST));
        assertEquals(1, params.get(FORM_PARAM_TEST).length);
    }

    @Test
    public void charEncoding_getEncoding_expectNoEncodingWithoutContentType() {
         HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_SINGLE_COOKIE, null, null);
         try {
             request.setCharacterEncoding(StandardCharsets.UTF_8.name());
             // we have not specified a content type so the encoding will not be set
             assertEquals(null, request.getCharacterEncoding());
             assertEquals(null, request.getContentType());
         } catch (UnsupportedEncodingException e) {
             fail("Unsupported encoding");
             e.printStackTrace();
         }
    }

    @Test
    public void charEncoding_getEncoding_expectContentTypeOnly() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        // we have not specified a content type so the encoding will not be set
        assertEquals(null, request.getCharacterEncoding());
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void charEncoding_addCharEncodingTwice_expectSingleMediaTypeAndEncoding() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        // we have not specified a content type so the encoding will not be set
        assertEquals(null, request.getCharacterEncoding());
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());

        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());


            request.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.ISO_8859_1.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.ISO_8859_1.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void contentType_lowerCaseHeaderKey_expectUpdatedMediaType() {
        HttpServletRequest request = new AwsProxyHttpServletRequest(REQUEST_WITH_LOWERCASE_HEADER, null, null);
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());

        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void contentType_duplicateCase_expectSingleContentTypeHeader() {
        AwsProxyRequest proxyRequest = getRequestWithHeaders();
        HttpServletRequest request = new AwsProxyHttpServletRequest(proxyRequest, null, null);

        try {
            request.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault())));
        } catch (UnsupportedEncodingException e) {
            fail("Unsupported encoding");
            e.printStackTrace();
        }
    }

    @Test
    public void requestURL_getUrl_expectHttpSchemaAndLocalhostForLocalTesting() {
        AwsProxyRequest req = getRequestWithHeaders();
        req.getRequestContext().setApiId("test-id");
        LambdaContainerHandler.getContainerConfig().enableLocalhost();
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("http://"));
        assertTrue(requestUrl.contains("test-id.execute-api."));
        assertTrue(requestUrl.endsWith(".com/hello"));

        // set localhost
        req.getMultiValueHeaders().putSingle("Host", "localhost");
        servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("http://localhost"));
        assertTrue(requestUrl.endsWith("localhost/hello"));
        LambdaContainerHandler.getContainerConfig().getCustomDomainNames().remove("localhost");
    }

    @Test
    public void requestURL_getUrlWithCustomBasePath_expectCustomBasePath() {
        AwsProxyRequest req = getRequestWithHeaders();
        LambdaContainerHandler.getContainerConfig().setServiceBasePath("test");
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("/test/hello"));
        LambdaContainerHandler.getContainerConfig().setServiceBasePath(null);
    }

    @Test
    public void requestURL_getUrlWithContextPath_expectStageAsContextPath() {
        AwsProxyRequest req = getRequestWithHeaders();
        req.getRequestContext().setStage("test-stage");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        System.out.println("Url: " + requestUrl);
        assertTrue(requestUrl.contains("/test-stage/"));
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @Test
    public void getLocales_emptyAcceptHeader_expectDefaultLocale() {
        AwsProxyRequest req = getRequestWithHeaders();
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        Enumeration<Locale> locales = servletRequest.getLocales();
        int localesNo = 0;
        while (locales.hasMoreElements()) {
            Locale defaultLocale = locales.nextElement();
            assertEquals(Locale.getDefault(), defaultLocale);
            localesNo++;
        }
        assertEquals(1, localesNo);
    }

    @Test
    public void getLocales_validAcceptHeader_expectSingleLocale() {
        AwsProxyRequest req = getRequestWithHeaders();
        req.getMultiValueHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH");
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        Enumeration<Locale> locales = servletRequest.getLocales();
        int localesNo = 0;
        while (locales.hasMoreElements()) {
            Locale defaultLocale = locales.nextElement();
            assertEquals(new Locale("fr-CH"), defaultLocale);
            localesNo++;
        }
        assertEquals(1, localesNo);
    }

    @Test
    public void getLocales_validAcceptHeaderMultipleLocales_expectFullLocaleList() {
        AwsProxyRequest req = getRequestWithHeaders();
        req.getMultiValueHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        Enumeration<Locale> locales = servletRequest.getLocales();
        List<Locale> localesList = new ArrayList<>();
        while (locales.hasMoreElements()) {
            localesList.add(locales.nextElement());
        }
        assertEquals(5, localesList.size());
        assertEquals(new Locale("fr-CH"), localesList.get(0));
        assertEquals(new Locale("fr"), localesList.get(1));
        assertEquals(new Locale("en"), localesList.get(2));
        assertEquals(new Locale("de"), localesList.get(3));
        assertEquals(new Locale("*"), localesList.get(4));

        assertNotNull(servletRequest.getLocale());
        assertEquals(new Locale("fr-CH"), servletRequest.getLocale());
    }

    @Test
    public void getLocales_validAcceptHeaderMultipleLocales_expectFullLocaleListOrdered() {
        AwsProxyRequest req = getRequestWithHeaders();
        req.getMultiValueHeaders().putSingle(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH, en;q=0.8, de;q=0.7, *;q=0.5, fr;q=0.9");
        HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req, null, null);
        Enumeration<Locale> locales = servletRequest.getLocales();
        List<Locale> localesList = new ArrayList<>();
        while (locales.hasMoreElements()) {
            localesList.add(locales.nextElement());
        }
        assertEquals(5, localesList.size());
        assertEquals(new Locale("fr-CH"), localesList.get(0));
        assertEquals(new Locale("fr"), localesList.get(1));
        assertEquals(new Locale("en"), localesList.get(2));
        assertEquals(new Locale("de"), localesList.get(3));
        assertEquals(new Locale("*"), localesList.get(4));
    }

    @Test
    public void nullQueryString_expectNoExceptions() {
        AwsProxyRequest req = new AwsProxyRequestBuilder("/hello", "GET").build();
        AwsProxyHttpServletRequest servletReq = new AwsProxyHttpServletRequest(req, null, null);
        assertNull(servletReq.getQueryString());
        assertEquals(0, servletReq.getParameterMap().size());
        assertFalse(servletReq.getParameterNames().hasMoreElements());
        assertNull(servletReq.getParameter("param"));
        assertNull(servletReq.getParameterValues("param"));
    }

    @Test
    public void inputStream_emptyBody_expectNullInputStream() {
        AwsProxyRequest proxyReq = getRequestWithHeaders();
        assertNull(proxyReq.getBody());
        HttpServletRequest req = new AwsProxyHttpServletRequest(proxyReq, null, null);

        try {
            InputStream is = req.getInputStream();
            assertTrue(is.getClass() == AwsProxyHttpServletRequest.AwsServletInputStream.class);
            assertEquals(0, is.available());
        } catch (IOException e) {
            fail("Could not get input stream");
        }
    }

    @Test
    public void getHeaders_emptyHeaders_expectEmptyEnumeration() {
        AwsProxyRequest proxyReq = new AwsProxyRequestBuilder("/hello", "GET").build();
        HttpServletRequest req = new AwsProxyHttpServletRequest(proxyReq, null, null);
        assertFalse(req.getHeaders("param").hasMoreElements());
    }

    @Test
    public void getServerPort_defaultPort_expect443() {
        HttpServletRequest req = new AwsProxyHttpServletRequest(getRequestWithHeaders(), null, null);
        assertEquals(443, req.getServerPort());
    }

    @Test
    public void getServerPort_customPortFromHeader_expectCustomPort() {
        AwsProxyRequest proxyReq = getRequestWithHeaders();
        proxyReq.getMultiValueHeaders().putSingle(AwsProxyHttpServletRequest.PORT_HEADER_NAME, "80");
        HttpServletRequest req = new AwsProxyHttpServletRequest(proxyReq, null, null);
        assertEquals(80, req.getServerPort());
    }

    @Test
    public void getServerPort_invalidCustomPortFromHeader_expectDefaultPort() {
        AwsProxyRequest proxyReq = getRequestWithHeaders();
        proxyReq.getMultiValueHeaders().putSingle(AwsProxyHttpServletRequest.PORT_HEADER_NAME, "7200");
        HttpServletRequest req = new AwsProxyHttpServletRequest(proxyReq, null, null);
        assertEquals(443, req.getServerPort());
    }


    private AwsProxyRequest getRequestWithHeaders() {
        return new AwsProxyRequestBuilder("/hello", "GET")
                       .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                       .header(AwsProxyHttpServletRequest.CF_PROTOCOL_HEADER_NAME, REQUEST_SCHEME_HTTP)
                       .build();
    }
}
