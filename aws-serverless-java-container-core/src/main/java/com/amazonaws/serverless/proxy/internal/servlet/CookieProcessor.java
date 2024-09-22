package com.amazonaws.serverless.proxy.internal.servlet;

import jakarta.servlet.http.Cookie;

public interface CookieProcessor {
    /**
     * Parse the provided cookie header value into an array of Cookie objects.
     *
     * @param cookieHeader The cookie header value string to parse, e.g., "SID=31d4d96e407aad42; lang=en-US"
     * @return An array of Cookie objects parsed from the cookie header value
     */
    Cookie[] parseCookieHeader(String cookieHeader);

    /**
     * Generate the Set-Cookie HTTP header value for the given Cookie.
     *
     * @param cookie The cookie for which the header will be generated
     * @return The header value in a form that can be added directly to the response
     */
    String generateHeader(Cookie cookie);


}
