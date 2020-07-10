package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;

import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.IOException;
import java.io.PrintWriter;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.concurrent.CountDownLatch;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


public class AwsHttpServletResponseTest {
    // we use this int to compare the cookie expiration time in the tests. The date we generate to compare to
    // may be slight off compared to the date generated during the request processing
    private static final int COOKIE_GRACE_COMPARE_MILLIS = 2000;
    private static final String COOKIE_NAME = "session_id";
    private static final String COOKIE_VALUE = "123";
    private static final String COOKIE_PATH = "/api";
    private static final String COOKIE_DOMAIN = "mydomain.com";
    private static final int MAX_AGE_VALUE = 300;

    private static final Pattern MAX_AGE_PATTERN = Pattern.compile("Max-Age=(-?[0-9]+)");
    private static final Pattern EXPIRES_PATTERN = Pattern.compile("Expires=(.*)$");

    private static final String CONTENT_TYPE_WITH_CHARSET = "application/json; charset=UTF-8";

    @Test
    public void cookie_addCookie_verifyPath() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie pathCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        pathCookie.setPath(COOKIE_PATH);

        resp.addCookie(pathCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("Path=" + COOKIE_PATH));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));
    }

    @Test
    public void cookie_addCookie_verifySecure() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie secureCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        secureCookie.setSecure(true);

        resp.addCookie(secureCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("; Secure"));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));
    }

    @Test
    public void cookie_addCookie_verifyDomain() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie domainCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        domainCookie.setDomain(COOKIE_DOMAIN);

        resp.addCookie(domainCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("; Domain=" + COOKIE_DOMAIN));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));
    }

    @Test
    public void cookie_addCookie_defaultMaxAgeIsNegative() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie maxAgeCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        maxAgeCookie.setDomain(COOKIE_DOMAIN);

        resp.addCookie(maxAgeCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertFalse(cookieHeader.contains("Max-Age="));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));
    }

    @Test
    public void cookie_addCookie_positiveMaxAgeIsPresent() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie maxAgeCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        maxAgeCookie.setMaxAge(MAX_AGE_VALUE);

        resp.addCookie(maxAgeCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("; Max-Age="));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));

        int maxAge = getMaxAge(cookieHeader);
        assertEquals(MAX_AGE_VALUE, maxAge);
    }

    @Test
    public void cookie_addCookie_positiveMaxAgeExpiresDate() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie maxAgeCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        maxAgeCookie.setMaxAge(MAX_AGE_VALUE);

        resp.addCookie(maxAgeCookie);
        Calendar testExpiration = Calendar.getInstance();
        testExpiration.add(Calendar.SECOND, MAX_AGE_VALUE);
        testExpiration.setTimeZone(TimeZone.getTimeZone(AwsHttpServletResponse.COOKIE_DEFAULT_TIME_ZONE));

        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("; Max-Age="));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));

        SimpleDateFormat dateFormat = new SimpleDateFormat(AwsHttpServletResponse.HEADER_DATE_PATTERN);

        Calendar expiration = getExpires(cookieHeader);

        long dateDiff = testExpiration.getTimeInMillis() - expiration.getTimeInMillis();
        assertTrue(Math.abs(dateDiff) < COOKIE_GRACE_COMPARE_MILLIS);
    }

    @Test
    public void cookie_addCookieWithoutMaxAge_expectNoExpires() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie simpleCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        resp.addCookie(simpleCookie);

        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        assertNotNull(cookieHeader);
        assertFalse(cookieHeader.contains("Expires"));
    }

    @Test
    public void responseHeaders_getAwsResponseHeaders_expectLatestHeader() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.addHeader(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
        resp.addHeader("content-type", "application/xml");

        Headers awsResp = resp.getAwsResponseHeaders();
        assertEquals(1, awsResp.size());
        assertEquals("application/xml", awsResp.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void responseHeaders_getAwsResponseHeaders_expectedMultpleCookieHeaders() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.addCookie(new Cookie(COOKIE_NAME, COOKIE_VALUE));
        resp.addCookie(new Cookie("Second", "test"));

        Headers awsResp = resp.getAwsResponseHeaders();
        assertEquals(1, awsResp.size());
        assertEquals(2, awsResp.get(HttpHeaders.SET_COOKIE).size());
    }

    @Test
    public void releaseLatch_flushBuffer_expectFlushToWriteAndRelease() {
        CountDownLatch respLatch = new CountDownLatch(1);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, respLatch);
        String respBody = "Test resp";
        PrintWriter writer = null;
        try {
            writer = resp.getWriter();
            PrintWriter finalWriter = writer;
            Runnable bodyWriter = () -> {
                finalWriter.write(respBody);
                try {
                    resp.flushBuffer();
                } catch (IOException e) {
                    fail("Could not flush buffer");
                }
            };

            new Thread(bodyWriter).start();
        } catch (IOException e) {
            fail("Could not get writer");
        }

        try {
            respLatch.await();
        } catch (InterruptedException e) {
            e.printStackTrace();
            fail("Response latch interrupted");
        }

        assertEquals(0, respLatch.getCount());
        assertNotNull(writer);
        assertEquals(respBody, resp.getAwsResponseBodyString());
    }

    @Test
    public void dateHeader_addDateHeader_expectMultipleHeaders() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.addDateHeader("Date", Instant.now().toEpochMilli());
        resp.addDateHeader("Date", Instant.now().toEpochMilli() - 1000);

        assertEquals(2, resp.getHeaders("Date").size());
    }

    @Test
    public void dateHeader_setDateHeader_expectSingleHeader() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setDateHeader("Date", Instant.now().toEpochMilli());
        resp.setDateHeader("Date", Instant.now().toEpochMilli() - 1000);

        assertEquals(1, resp.getHeaders("Date").size());
    }

    @Test
    public void response_reset_expectEmptyHeadersAndBody() {
        CountDownLatch respLatch = new CountDownLatch(1);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, respLatch);
        String body = "My Body";

        resp.addHeader("Test", "test");
        try {
            resp.getWriter().write(body);
            resp.flushBuffer();
        } catch (IOException e) {
            fail("Could not get writer");
        }

        assertEquals(1, resp.getHeaderNames().size());
        assertEquals(body, resp.getAwsResponseBodyString());

        resp.reset();

        assertEquals(0, resp.getHeaderNames().size());
    }

    @Test
    public void headers_setIntHeader_expectSingleHeaderValue() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setIntHeader("Test", 15);
        resp.setIntHeader("Test", 34);

        assertEquals(1, resp.getHeaderNames().size());
        assertEquals(1, resp.getHeaders("Test").size());
        assertEquals("34", resp.getHeader("Test"));
    }

    @Test
    public void headers_addIntHeader_expectMultipleHeaderValues() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.addIntHeader("Test", 15);
        resp.addIntHeader("Test", 34);

        assertEquals(1, resp.getHeaderNames().size());
        assertEquals(2, resp.getHeaders("Test").size());
        assertEquals("15", resp.getHeader("Test"));
    }

    @Test
    public void characterEncoding_setCharacterEncoding() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");
        assertNotEquals("UTF-8", resp.getHeader("Content-Encoding"));
        assertEquals("application/json; charset=UTF-8", resp.getContentType());
        assertEquals("application/json; charset=UTF-8", resp.getHeader("Content-Type"));
    }

    @Test
    public void characterEncoding_setContentType() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setContentType("application/json; charset=utf-8");
        resp.setCharacterEncoding("UTF-8");

        assertEquals("application/json; charset=UTF-8", resp.getContentType());
        assertEquals("application/json; charset=UTF-8", resp.getHeader("Content-Type"));
        assertEquals("UTF-8", resp.getCharacterEncoding());
    }

    @Test
    public void characterEncoding_setContentTypeAndsetCharacterEncoding() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setContentType("application/json");
        resp.setCharacterEncoding("UTF-8");

        assertEquals("application/json; charset=UTF-8", resp.getContentType());
        assertEquals("application/json; charset=UTF-8", resp.getHeader("Content-Type"));
        assertEquals("UTF-8", resp.getCharacterEncoding());
    }

    @Test
    public void characterEncoding_setCharacterEncodingAndsetContentType() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setCharacterEncoding("UTF-8");
        resp.setContentType("application/json");

        assertEquals("application/json; charset=UTF-8", resp.getContentType());
        assertEquals("application/json; charset=UTF-8", resp.getHeader("Content-Type"));
        assertEquals("UTF-8", resp.getCharacterEncoding());
    }

    @Test
    public void characterEncoding_setCharacterEncodingInContentType_characterEncodingPopulatedCorrectly() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setContentType(CONTENT_TYPE_WITH_CHARSET);

        assertEquals(CONTENT_TYPE_WITH_CHARSET, resp.getContentType());
        assertEquals(CONTENT_TYPE_WITH_CHARSET, resp.getHeader("Content-Type"));
        assertEquals("UTF-8", resp.getCharacterEncoding());
    }

    @Test
    public void characterEncoding_setCharacterEncodingInContentType_overridesDefault() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        resp.setCharacterEncoding(ContainerConfig.DEFAULT_CONTENT_CHARSET);
        resp.setContentType(CONTENT_TYPE_WITH_CHARSET);

        assertEquals(CONTENT_TYPE_WITH_CHARSET, resp.getContentType());
        assertEquals(CONTENT_TYPE_WITH_CHARSET, resp.getHeader("Content-Type"));
        assertEquals("UTF-8", resp.getCharacterEncoding());
    }

    private int getMaxAge(String header) {
        Matcher ageMatcher = MAX_AGE_PATTERN.matcher(header);
        assertTrue(ageMatcher.find());
        assertTrue(ageMatcher.groupCount() >= 1);
        String ageString = ageMatcher.group(1);
        return Integer.parseInt(ageString);
    }

    private Calendar getExpires(String header) {
        Matcher ageMatcher = EXPIRES_PATTERN.matcher(header);
        assertTrue(ageMatcher.find());
        assertTrue(ageMatcher.groupCount() >= 1);
        String expiresString = ageMatcher.group(1);
        SimpleDateFormat sdf = new SimpleDateFormat(AwsHttpServletResponse.HEADER_DATE_PATTERN);
        Calendar cal = Calendar.getInstance();
        try {
            cal.setTime(sdf.parse(expiresString));
        } catch (ParseException e) {
            e.printStackTrace();
            fail("Could not parse expire date");
        }

        return cal;
    }
}
