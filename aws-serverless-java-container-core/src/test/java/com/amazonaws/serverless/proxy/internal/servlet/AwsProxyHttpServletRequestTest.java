package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;

import com.amazonaws.services.lambda.runtime.Context;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.SecurityContext;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.ZonedDateTime;
import java.util.*;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

@RunWith(Parameterized.class)
public class AwsProxyHttpServletRequestTest {
    private String requestType;

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

    private static final AwsProxyRequestBuilder REQUEST_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_INVALID_FORM_URLENCODED = new AwsProxyRequestBuilder("/hello", "GET")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_FORM_URLENCODED_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_SINGLE_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_MULTIPLE_COOKIES = new AwsProxyRequestBuilder("/hello", "GET")
            .cookie(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .cookie(FORM_PARAM_TEST, FORM_PARAM_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_MALFORMED_COOKIE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.COOKIE, QUERY_STRING_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_MULTIPLE_FORM_AND_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .form(FORM_PARAM_NAME, FORM_PARAM_NAME_VALUE)
            .queryString(FORM_PARAM_TEST, QUERY_STRING_NAME_VALUE);
    private static final AwsProxyRequestBuilder REQUEST_USER_AGENT_REFERER = new AwsProxyRequestBuilder("/hello", "POST")
            .userAgent(USER_AGENT)
            .referer(REFERER);
    private static final AwsProxyRequestBuilder REQUEST_WITH_DATE = new AwsProxyRequestBuilder("/hello", "GET")
            .header(HttpHeaders.DATE, AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE));
    private static final AwsProxyRequestBuilder REQUEST_WITH_LOWERCASE_HEADER = new AwsProxyRequestBuilder("/hello", "POST")
            .header(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault()), MediaType.APPLICATION_JSON);

    private static final AwsProxyRequestBuilder REQUEST_NULL_QUERY_STRING;
    static {
        AwsProxyRequest awsProxyRequest = new AwsProxyRequestBuilder("/hello", "GET").build();
        awsProxyRequest.setMultiValueQueryStringParameters(null);
        REQUEST_NULL_QUERY_STRING = new AwsProxyRequestBuilder(awsProxyRequest);
    }

    private static final AwsProxyRequestBuilder REQUEST_QUERY = new AwsProxyRequestBuilder("/hello", "POST")
            .queryString(FORM_PARAM_NAME, QUERY_STRING_NAME_VALUE);


    public AwsProxyHttpServletRequestTest(String type) {
        requestType = type;
    }

    @Parameterized.Parameters
    public static Collection<Object> data() {
        return Arrays.asList(new Object[] { "API_GW", "ALB", "HTTP_API", "WRAP" });
    }

    private HttpServletRequest getRequest(AwsProxyRequestBuilder req, Context lambdaCtx, SecurityContext securityCtx) {
        switch (requestType) {
            case "API_GW":
                return new AwsProxyHttpServletRequest(req.build(), lambdaCtx, securityCtx);
            case "ALB":
                return new AwsProxyHttpServletRequest(req.alb().build(), lambdaCtx, securityCtx);
            case "HTTP_API":
                return new AwsHttpApiV2ProxyHttpServletRequest(req.toHttpApiV2Request(), lambdaCtx, securityCtx, LambdaContainerHandler.getContainerConfig());
            case "WRAP":
                HttpServletRequest servletRequest = new AwsProxyHttpServletRequest(req.build(), lambdaCtx, securityCtx);
                return new AwsHttpServletRequestWrapper(servletRequest, req.build().getPath());
            default:
                throw new RuntimeException("Unknown test variant: " + requestType);
        }
    }


    @Test
    public void headers_getHeader_validRequest() {
        HttpServletRequest request = getRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(CUSTOM_HEADER_VALUE, request.getHeader(CUSTOM_HEADER_KEY));
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
    }

    @Test
    public void headers_getRefererAndUserAgent_returnsContextValues() {
        assumeFalse("ALB".equals(requestType));
        HttpServletRequest request = getRequest(REQUEST_USER_AGENT_REFERER, null, null);
        assertNotNull(request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("Referer"));
        assertEquals(REFERER, request.getHeader("referer"));

        assertNotNull(request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("User-Agent"));
        assertEquals(USER_AGENT, request.getHeader("user-agent"));
    }

    @Test
    public void formParams_getParameter_validForm() {
        HttpServletRequest request = getRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getParameter(FORM_PARAM_NAME));
        assertEquals(FORM_PARAM_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_null() {
        HttpServletRequest request = getRequest(REQUEST_INVALID_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNull(request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void formParams_getParameter_multipleParams() {
        HttpServletRequest request = getRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
    }

    @Test
    public void formParams_getParameter_queryStringPrecendence() {
        HttpServletRequest request = getRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(2, request.getParameterValues(FORM_PARAM_NAME).length);
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameter(FORM_PARAM_NAME));
    }

    @Test
    public void dateHeader_noDate_returnNegativeOne() {
        HttpServletRequest request = getRequest(REQUEST_FORM_URLENCODED_AND_QUERY, null, null);
        assertNotNull(request);
        assertEquals(-1L, request.getDateHeader(HttpHeaders.DATE));
    }

    @Test
    public void dateHeader_correctDate_parseToCorrectLong() {
        HttpServletRequest request = getRequest(REQUEST_WITH_DATE, null, null);
        assertNotNull(request);

        String instantString = AwsHttpServletRequest.dateFormatter.format(REQUEST_DATE);
        assertEquals(Instant.from(AwsHttpServletRequest.dateFormatter.parse(instantString)).toEpochMilli(), request.getDateHeader(HttpHeaders.DATE));
        assertEquals(-1L, request.getDateHeader(HttpHeaders.IF_MODIFIED_SINCE));
    }

    @Test
    public void scheme_getScheme_https() {
        HttpServletRequest request = getRequest(REQUEST_FORM_URLENCODED, null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals("https", request.getScheme());
    }

    @Test
    public void scheme_getScheme_http() {
        HttpServletRequest request = getRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getScheme());
        assertEquals(REQUEST_SCHEME_HTTP, request.getScheme());
    }

    @Test
    public void cookie_getCookies_noCookies() {
        HttpServletRequest request = getRequest(getRequestWithHeaders(), null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void cookie_getCookies_singleCookie() {
        HttpServletRequest request = getRequest(REQUEST_SINGLE_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(1, request.getCookies().length);
        assertEquals(FORM_PARAM_NAME, request.getCookies()[0].getName());
        assertEquals(FORM_PARAM_NAME_VALUE, request.getCookies()[0].getValue());
    }

    @Test
    public void cookie_getCookies_multipleCookies() {
        HttpServletRequest request = getRequest(REQUEST_MULTIPLE_COOKIES, null, null);
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
        HttpServletRequest request = getRequest(REQUEST_MALFORMED_COOKIE, null, null);
        assertNotNull(request);
        assertNotNull(request.getCookies());
        assertEquals(0, request.getCookies().length);
    }

    @Test
    public void queryParameters_getParameterMap_null() {
        HttpServletRequest request = getRequest(REQUEST_NULL_QUERY_STRING, null, null);
        assertNotNull(request);
        assertEquals(0, request.getParameterMap().size());
    }

    @Test
    public void queryParameters_getParameterMap_nonNull() {
        HttpServletRequest request = getRequest(REQUEST_QUERY, null, null);
        assertNotNull(request);
        assertEquals(1, request.getParameterMap().size());
        assertEquals(QUERY_STRING_NAME_VALUE, request.getParameterMap().get(FORM_PARAM_NAME)[0]);
    }

    @Test
    public void queryParameters_getParameterNames_null() {
        HttpServletRequest request = getRequest(REQUEST_NULL_QUERY_STRING, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(0, parameterNames.size());
    }

    @Test
    public void queryParameters_getParameterNames_notNull() {
        HttpServletRequest request = getRequest(REQUEST_QUERY, null, null);
        List<String> parameterNames = Collections.list(request.getParameterNames());
        assertNotNull(request);
        assertEquals(1, parameterNames.size());
        assertTrue(parameterNames.contains(FORM_PARAM_NAME));
    }

    @Test
    public void queryParameter_getParameterMap_avoidDuplicationOnMultipleCalls() {
        HttpServletRequest request = getRequest(REQUEST_MULTIPLE_FORM_AND_QUERY, null, null);

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
         HttpServletRequest request = getRequest(REQUEST_SINGLE_COOKIE, null, null);
         try {
             request.setCharacterEncoding(StandardCharsets.UTF_8.name());
             // we have not specified a content type so the encoding will not be set
             assertNull(request.getCharacterEncoding());
             assertNull(request.getContentType());
         } catch (UnsupportedEncodingException e) {
             e.printStackTrace();
             fail("Unsupported encoding");

         }
    }

    @Test
    public void charEncoding_getEncoding_expectContentTypeOnly() {
        HttpServletRequest request = getRequest(getRequestWithHeaders(), null, null);
        // we have not specified a content type so the encoding will not be set
        assertNull(request.getCharacterEncoding());
        assertEquals(MediaType.APPLICATION_JSON, request.getContentType());
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Unsupported encoding");
        }
    }

    @Test
    public void charEncoding_addCharEncodingTwice_expectSingleMediaTypeAndEncoding() {
        HttpServletRequest request = getRequest(getRequestWithHeaders(), null, null);
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
            e.printStackTrace();
            fail("Unsupported encoding");
        }
    }

    @Test
    public void contentType_lowerCaseHeaderKey_expectUpdatedMediaType() {
        HttpServletRequest request = getRequest(REQUEST_WITH_LOWERCASE_HEADER, null, null);
        try {
            request.setCharacterEncoding(StandardCharsets.UTF_8.name());
            String newHeaderValue = MediaType.APPLICATION_JSON + "; charset=" + StandardCharsets.UTF_8.name();
            assertEquals(newHeaderValue, request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertEquals(newHeaderValue, request.getContentType());
            assertEquals(StandardCharsets.UTF_8.name(), request.getCharacterEncoding());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Unsupported encoding");
        }
    }

    @Test
    public void contentType_duplicateCase_expectSingleContentTypeHeader() {
        AwsProxyRequestBuilder proxyRequest = getRequestWithHeaders();
        HttpServletRequest request = getRequest(proxyRequest, null, null);

        try {
            request.setCharacterEncoding(StandardCharsets.ISO_8859_1.name());
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE));
            assertNotNull(request.getHeader(HttpHeaders.CONTENT_TYPE.toLowerCase(Locale.getDefault())));
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            fail("Unsupported encoding");
        }
    }

    @Test
    public void requestURL_getUrl_expectHttpSchemaAndLocalhostForLocalTesting() {
        assumeFalse("ALB".equals(requestType));
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        req.apiId("test-id");
        LambdaContainerHandler.getContainerConfig().enableLocalhost();
        HttpServletRequest servletRequest = getRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("http://"));
        assertTrue(requestUrl.contains("test-id.execute-api."));
        assertTrue(requestUrl.endsWith(".com/hello"));

        // set localhost
        req.header("Host", "localhost");
        servletRequest = getRequest(req, null, null);
        requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("http://localhost"));
        assertTrue(requestUrl.endsWith("localhost/hello"));
        LambdaContainerHandler.getContainerConfig().getCustomDomainNames().remove("localhost");
    }

    @Test
    public void requestURL_getUrlWithCustomBasePath_expectCustomBasePath() {
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        LambdaContainerHandler.getContainerConfig().setServiceBasePath("test");
        HttpServletRequest servletRequest = getRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        assertTrue(requestUrl.contains("/test/hello"));
        LambdaContainerHandler.getContainerConfig().setServiceBasePath(null);
    }

    @Test
    public void requestURL_getUrlWithContextPath_expectStageAsContextPath() {
        assumeFalse("ALB".equals(requestType));
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        req.stage("test-stage");
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(true);
        HttpServletRequest servletRequest = getRequest(req, null, null);
        String requestUrl = servletRequest.getRequestURL().toString();
        System.out.println(requestUrl);
        assertTrue(requestUrl.contains("/test-stage/"));
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    @Test
    public void getLocales_emptyAcceptHeader_expectDefaultLocale() {
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        HttpServletRequest servletRequest = getRequest(req, null, null);
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
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        req.header(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH");
        HttpServletRequest servletRequest = getRequest(req, null, null);
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
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        req.header(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH, fr;q=0.9, en;q=0.8, de;q=0.7, *;q=0.5");
        HttpServletRequest servletRequest = getRequest(req, null, null);
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
        AwsProxyRequestBuilder req = getRequestWithHeaders();
        req.header(HttpHeaders.ACCEPT_LANGUAGE, "fr-CH, en;q=0.8, de;q=0.7, *;q=0.5, fr;q=0.9");
        HttpServletRequest servletRequest = getRequest(req, null, null);
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
        AwsProxyRequestBuilder req = new AwsProxyRequestBuilder("/hello", "GET");
        HttpServletRequest servletReq = getRequest(req, null, null);
        assertNull(servletReq.getQueryString());
        assertEquals(0, servletReq.getParameterMap().size());
        assertFalse(servletReq.getParameterNames().hasMoreElements());
        assertNull(servletReq.getParameter("param"));
        assertNull(servletReq.getParameterValues("param"));
    }

    @Test
    public void inputStream_emptyBody_expectNullInputStream() {
        AwsProxyRequestBuilder proxyReq = getRequestWithHeaders();
        assertNull(proxyReq.build().getBody());
        HttpServletRequest req = getRequest(proxyReq, null, null);

        try {
            InputStream is = req.getInputStream();
            assertTrue(is.getClass() == AwsServletInputStream.class);
            assertEquals(0, is.available());
        } catch (IOException e) {
            fail("Could not get input stream");
        }
    }

    @Test
    public void getHeaders_emptyHeaders_expectEmptyEnumeration() {
        AwsProxyRequestBuilder proxyReq = new AwsProxyRequestBuilder("/hello", "GET");
        HttpServletRequest req = getRequest(proxyReq, null, null);
        assertFalse(req.getHeaders("param").hasMoreElements());
    }

    @Test
    public void getServerPort_defaultPort_expect443() {
        HttpServletRequest req = getRequest(getRequestWithHeaders(), null, null);
        assertEquals(443, req.getServerPort());
    }

    @Test
    public void getServerPort_customPortFromHeader_expectCustomPort() {
        AwsProxyRequestBuilder proxyReq = getRequestWithHeaders();
        proxyReq.header(AwsProxyHttpServletRequest.PORT_HEADER_NAME, "80");
        HttpServletRequest req = getRequest(proxyReq, null, null);
        assertEquals(80, req.getServerPort());
    }

    @Test
    public void getServerPort_invalidCustomPortFromHeader_expectDefaultPort() {
        AwsProxyRequestBuilder proxyReq = getRequestWithHeaders();
        proxyReq.header(AwsProxyHttpServletRequest.PORT_HEADER_NAME, "7200");
        HttpServletRequest req = getRequest(proxyReq, null, null);
        assertEquals(443, req.getServerPort());
    }

    @Test
    public void serverName_emptyHeaders_doesNotThrowNullPointer() {
        AwsProxyRequestBuilder proxyReq = new AwsProxyRequestBuilder("/test", "GET");
        proxyReq.multiValueHeaders(null);
        HttpServletRequest servletReq = getRequest(proxyReq, null, null);
        String serverName = servletReq.getServerName();
        assertTrue(serverName.startsWith("null.execute-api"));
    }

    @Test
    public void serverName_hostHeader_returnsHostHeaderOnly() {
        AwsProxyRequestBuilder proxyReq = new AwsProxyRequestBuilder("/test", "GET")
                .header(HttpHeaders.HOST, "testapi.com");
        LambdaContainerHandler.getContainerConfig().addCustomDomain("testapi.com");
        HttpServletRequest servletReq = getRequest(proxyReq, null, null);
        String serverName = servletReq.getServerName();
        assertEquals("testapi.com", serverName);
    }

    private AwsProxyRequestBuilder getRequestWithHeaders() {
        return new AwsProxyRequestBuilder("/hello", "GET")
                       .header(CUSTOM_HEADER_KEY, CUSTOM_HEADER_VALUE)
                       .header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON)
                       .header(AwsProxyHttpServletRequest.CF_PROTOCOL_HEADER_NAME, REQUEST_SCHEME_HTTP);
    }
}
