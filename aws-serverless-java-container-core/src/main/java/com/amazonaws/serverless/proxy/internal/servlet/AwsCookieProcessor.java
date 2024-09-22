package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import jakarta.servlet.http.Cookie;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.DateFormat;
import java.text.FieldPosition;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Implementation of the CookieProcessor interface that provides cookie parsing and generation functionality.
 */
public class AwsCookieProcessor implements CookieProcessor {

    // Cookie attribute constants
    static final String COOKIE_COMMENT_ATTR = "Comment";
    static final String COOKIE_DOMAIN_ATTR = "Domain";
    static final String COOKIE_MAX_AGE_ATTR = "Max-Age";
    static final String COOKIE_PATH_ATTR = "Path";
    static final String COOKIE_SECURE_ATTR = "Secure";
    static final String COOKIE_HTTP_ONLY_ATTR = "HttpOnly";
    static final String COOKIE_SAME_SITE_ATTR = "SameSite";
    static final String COOKIE_PARTITIONED_ATTR = "Partitioned";
    static final String EMPTY_STRING = "";

    // BitSet to store valid token characters as defined in RFC 2616
    static final BitSet tokenValid = createTokenValidSet();

    // BitSet to validate domain characters
    static final BitSet domainValid = createDomainValidSet();

    static final String COOKIE_DATE_PATTERN = "EEE, dd MMM yyyy HH:mm:ss z";

    // ThreadLocal to ensure thread-safe creation of DateFormat instances for each thread
    static final ThreadLocal<DateFormat> COOKIE_DATE_FORMAT = ThreadLocal.withInitial(() -> {
        DateFormat df = new SimpleDateFormat(COOKIE_DATE_PATTERN, Locale.US);
        df.setTimeZone(TimeZone.getTimeZone("GMT"));
        return df;
    });

    static final String ANCIENT_DATE = COOKIE_DATE_FORMAT.get().format(new Date(10000));

    static BitSet createTokenValidSet() {
        BitSet tokenSet = new BitSet(128);
        for (char c = '0'; c <= '9'; c++) tokenSet.set(c);
        for (char c = 'a'; c <= 'z'; c++) tokenSet.set(c);
        for (char c = 'A'; c <= 'Z'; c++) tokenSet.set(c);
        for (char c : "!#$%&'*+-.^_`|~".toCharArray()) tokenSet.set(c);
        return tokenSet;
    }

    static BitSet createDomainValidSet() {
        BitSet domainValid = new BitSet(128);
        for (char c = '0'; c <= '9'; c++) domainValid.set(c);
        for (char c = 'a'; c <= 'z'; c++) domainValid.set(c);
        for (char c = 'A'; c <= 'Z'; c++) domainValid.set(c);
        domainValid.set('.');
        domainValid.set('-');
        return domainValid;
    }

    private final Logger log = LoggerFactory.getLogger(AwsCookieProcessor.class);

    @Override
    public Cookie[] parseCookieHeader(String cookieHeader) {
        // Return an empty array if the input is null or empty after trimming
        if (cookieHeader == null || cookieHeader.trim().isEmpty()) {
            return new Cookie[0];
        }

        // Parse cookie header and convert to Cookie array
        return Arrays.stream(cookieHeader.split("\\s*;\\s*"))
                .map(this::parseCookiePair)
                .filter(Objects::nonNull) // Filter out invalid pairs
                .toArray(Cookie[]::new);
    }

    /**
     * Parse a single cookie pair (name=value).
     *
     * @param cookiePair The cookie pair string.
     * @return A valid Cookie object or null if the pair is invalid.
     */
    private Cookie parseCookiePair(String cookiePair) {
        String[] kv = cookiePair.split("=", 2);

        if (kv.length != 2) {
            log.warn("Ignoring invalid cookie: {}", cookiePair);
            return null;  // Skip malformed cookie pairs
        }

        String cookieName = kv[0];
        String cookieValue = kv[1];

        // Validate name and value
        if (!isToken(cookieName)){
            log.warn("Ignoring cookie with invalid name: {}={}", cookieName, cookieValue);
            return null;  // Skip invalid cookie names
        }

        if (!isValidCookieValue(cookieValue)) {
            log.warn("Ignoring cookie with invalid value: {}={}", cookieName, cookieValue);
            return null;  // Skip invalid cookie values
        }

        // Return a new Cookie object after security processing
        return new Cookie(SecurityUtils.crlf(cookieName), SecurityUtils.crlf(cookieValue));
    }

    @Override
    public String generateHeader(Cookie cookie) {
        StringBuffer header = new StringBuffer();
        header.append(cookie.getName()).append('=');

        String value = cookie.getValue();
        if (value != null && value.length() > 0) {
            validateCookieValue(value);
            header.append(value);
        }

        int maxAge = cookie.getMaxAge();
        if (maxAge > -1) {
            header.append("; Expires=");
            if (maxAge == 0) {
                header.append(ANCIENT_DATE);
            } else {
                COOKIE_DATE_FORMAT.get().format(
                        new Date(System.currentTimeMillis() + maxAge * 1000L), header, new FieldPosition(0));
                header.append("; Max-Age=").append(maxAge);
            }
        }

        String domain = cookie.getDomain();
        if (domain != null && !domain.isEmpty()) {
            validateDomain(domain);
            header.append("; Domain=").append(domain);
        }

        String path = cookie.getPath();
        if (path != null && !path.isEmpty()) {
            validatePath(path);
            header.append("; Path=").append(path);
        }

        if (cookie.getSecure()) {
            header.append("; Secure");
        }

        if (cookie.isHttpOnly()) {
            header.append("; HttpOnly");
        }

        String sameSite = cookie.getAttribute(COOKIE_SAME_SITE_ATTR);
        if (sameSite != null) {
            header.append("; SameSite=").append(sameSite);
        }

        String partitioned = cookie.getAttribute(COOKIE_PARTITIONED_ATTR);
        if (EMPTY_STRING.equals(partitioned)) {
            header.append("; Partitioned");
        }

        addAdditionalAttributes(cookie, header);

        return header.toString();
    }

    private void addAdditionalAttributes(Cookie cookie, StringBuffer header) {
        for (Map.Entry<String, String> entry : cookie.getAttributes().entrySet()) {
            switch (entry.getKey()) {
                case COOKIE_COMMENT_ATTR:
                case COOKIE_DOMAIN_ATTR:
                case COOKIE_MAX_AGE_ATTR:
                case COOKIE_PATH_ATTR:
                case COOKIE_SECURE_ATTR:
                case COOKIE_HTTP_ONLY_ATTR:
                case COOKIE_SAME_SITE_ATTR:
                case COOKIE_PARTITIONED_ATTR:
                    // Already handled attributes are ignored
                    break;
                default:
                    validateAttribute(entry.getKey(), entry.getValue());
                    header.append("; ").append(entry.getKey());
                    if (!EMPTY_STRING.equals(entry.getValue())) {
                        header.append('=').append(entry.getValue());
                    }
                    break;
            }
        }
    }

    private void validateCookieValue(String value) {
        if (!isValidCookieValue(value)) {
            throw new IllegalArgumentException("Invalid cookie value: " + value);
        }
    }

    private void validateDomain(String domain) {
        if (!isValidDomain(domain)) {
            throw new IllegalArgumentException("Invalid cookie domain: " + domain);
        }
    }

    private void validatePath(String path) {
        for (char ch : path.toCharArray()) {
            if (ch < 0x20 || ch > 0x7E || ch == ';') {
                throw new IllegalArgumentException("Invalid cookie path: " + path);
            }
        }
    }

    private void validateAttribute(String name, String value) {
        if (!isToken(name)) {
            throw new IllegalArgumentException("Invalid cookie attribute name: " + name);
        }

        for (char ch : value.toCharArray()) {
            if (ch < 0x20 || ch > 0x7E || ch == ';') {
                throw new IllegalArgumentException("Invalid cookie attribute value: " + ch);
            }
        }
    }

    private boolean isValidCookieValue(String value) {
        int start = 0;
        int end = value.length();
        boolean quoted = end > 1 && value.charAt(0) == '"' && value.charAt(end - 1) == '"';

        char[] chars = value.toCharArray();
        for (int i = start; i < end; i++) {
            if (quoted && (i == start || i == end - 1)) {
                continue;
            }
            char c = chars[i];
            if (!isValidCookieChar(c)) return false;
        }
        return true;
    }

    private boolean isValidDomain(String domain) {
        if (domain.isEmpty()) {
            return false;
        }
        int prev = -1;
        for (char c : domain.toCharArray()) {
            if (!domainValid.get(c) || isInvalidLabelStartOrEnd(prev, c)) {
                return false;
            }
            prev = c;
        }
        return prev != '.' && prev != '-';
    }

    private boolean isInvalidLabelStartOrEnd(int prev, char current) {
        return (prev == '.' || prev == -1) && (current == '.' || current == '-') ||
                (prev == '-' && current == '.');
    }

    private boolean isToken(String s) {
        if (s.isEmpty()) return false;
        for (char c : s.toCharArray()) {
            if (!tokenValid.get(c)) {
                return false;
            }
        }
        return true;
    }

    private boolean isValidCookieChar(char c) {
        return !(c < 0x21 || c > 0x7E ||  c == 0x22 || c == 0x2c || c == 0x3b || c == 0x5c || c == 0x7f);
    }
}
