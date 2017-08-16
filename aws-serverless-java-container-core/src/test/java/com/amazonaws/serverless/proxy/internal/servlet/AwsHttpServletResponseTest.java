package com.amazonaws.serverless.proxy.internal.servlet;


import org.junit.Test;

import javax.servlet.http.Cookie;
import javax.ws.rs.core.HttpHeaders;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.TimeZone;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.Assert.*;


public class AwsHttpServletResponseTest {
    private static final String COOKIE_NAME = "session_id";
    private static final String COOKIE_VALUE = "123";
    private static final String COOKIE_PATH = "/api";
    private static final String COOKIE_DOMAIN = "mydomain.com";
    private static final int MAX_AGE_VALUE = 300;

    private static final Pattern MAX_AGE_PATTERN = Pattern.compile("Max-Age=(-?[0-9]+)");
    private static final Pattern EXPIRES_PATTERN = Pattern.compile("Expires=(.*)$");

    @Test
    public void cookie_addCookie_verifyPath() {
        AwsHttpServletResponse resp = new AwsHttpServletResponse(null, null);
        Cookie pathCookie = new Cookie(COOKIE_NAME, COOKIE_VALUE);
        pathCookie.setPath(COOKIE_PATH);

        resp.addCookie(pathCookie);
        String cookieHeader = resp.getHeader(HttpHeaders.SET_COOKIE);
        System.out.println("Cookie string: " + cookieHeader);
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
        System.out.println("Cookie string: " + cookieHeader);
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
        System.out.println("Cookie string: " + cookieHeader);
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
        System.out.println("Cookie string: " + cookieHeader);
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
        System.out.println("Cookie string: " + cookieHeader);
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
        System.out.println("Cookie string: " + cookieHeader);
        assertNotNull(cookieHeader);
        assertTrue(cookieHeader.contains("; Max-Age="));
        assertTrue(cookieHeader.contains(COOKIE_NAME + "=" + COOKIE_VALUE));

        SimpleDateFormat dateFormat = new SimpleDateFormat(AwsHttpServletResponse.HEADER_DATE_PATTERN);

        Calendar expiration = getExpires(cookieHeader);
        System.out.println("Cookie date: " + dateFormat.format(expiration.getTime()));
        System.out.println("Test date: " + dateFormat.format(testExpiration.getTime()));
        // we need to compare strings because the millis time will be off
        assertEquals(dateFormat.format(testExpiration.getTime()), dateFormat.format(expiration.getTime()));
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

    private int getMaxAge(String header) {
        Matcher ageMatcher = MAX_AGE_PATTERN.matcher(header);
        assertTrue(ageMatcher.find());
        assertTrue(ageMatcher.groupCount() >= 1);
        String ageString = ageMatcher.group(1);
        System.out.println("Age string: " + ageString);
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
