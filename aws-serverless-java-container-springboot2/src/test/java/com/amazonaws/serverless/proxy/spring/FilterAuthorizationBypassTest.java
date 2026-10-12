package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.filterauthapp.AdminAuthorizationFilter;
import com.amazonaws.serverless.proxy.spring.filterauthapp.AdminWildcardController;
import com.amazonaws.serverless.proxy.spring.filterauthapp.FilterAuthApplication;
import com.amazonaws.serverless.proxy.spring.filterauthapp.LambdaHandler;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import java.util.Arrays;
import java.util.Collection;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end regression test for the filter-selection authorization bypass, driven through the shipped Lambda entry
 * point rather than the filter matcher in isolation.
 *
 * The application protects /admin/* with a deny-by-default servlet Filter that never calls chain.doFilter, so the
 * response is unambiguous: 403 with FORBIDDEN means the filter was selected and ran, and 200 with TOP_SECRET means it
 * was skipped and the protected handler served the request.
 *
 * Before the fix, filter url-pattern matching used the raw undecoded path from getRequestURI() while servlet
 * resolution used the decoded path from getPathInfo(), so an encoded or dot-segment spelling of /admin/secret
 * returned TOP_SECRET with the filter never invoked.
 */
public class FilterAuthorizationBypassTest {
    private final MockLambdaContext lambdaContext = new MockLambdaContext();

    private LambdaHandler handler;

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    private AwsProxyResponse get(String reqType, String path) {
        handler = new LambdaHandler(reqType);
        AdminAuthorizationFilter.resetInvocations();
        return handler.handleRequest(new AwsProxyRequestBuilder(path, "GET"), lambdaContext);
    }

    private void assertProtected(String reqType, String path) {
        AwsProxyResponse resp = get(reqType, path);
        assertEquals(403, resp.getStatusCode(),
                "expected the filter to protect " + path + " on " + reqType + " but got " + resp.getBody());
        assertEquals(AdminAuthorizationFilter.FORBIDDEN_BODY, resp.getBody());
        assertNotEquals(FilterAuthApplication.SECRET_BODY, resp.getBody());
        assertTrue(AdminAuthorizationFilter.getInvocations() > 0,
                "the filter was never invoked for " + path + " on " + reqType);
    }

    /** Control: the plain protected path is blocked. If this fails, the app is not wired as the test assumes. */
    @MethodSource("data")
    @ParameterizedTest
    void plainProtectedPath_isBlocked(String reqType) {
        assertProtected(reqType, "/admin/secret");
    }

    /** Control: the filter mapping really is scoped, so an unprotected path is served normally. */
    @MethodSource("data")
    @ParameterizedTest
    void unprotectedPath_isServed(String reqType) {
        AwsProxyResponse resp = get(reqType, "/public/info");
        assertEquals(200, resp.getStatusCode());
        assertEquals(FilterAuthApplication.PUBLIC_BODY, resp.getBody());
        assertEquals(0, AdminAuthorizationFilter.getInvocations());
    }

    /** The reported bypass: %61 is an encoded "a", so this is /admin/secret. */
    @MethodSource("data")
    @ParameterizedTest
    void percentEncodedFirstCharacter_isBlocked(String reqType) {
        assertProtected(reqType, "/%61dmin/secret");
    }

    /** The same bypass with the encoding in the middle of the protected segment. */
    @MethodSource("data")
    @ParameterizedTest
    void percentEncodedMiddleCharacter_isBlocked(String reqType) {
        assertProtected(reqType, "/adm%69n/secret");
    }

    /** Every character of the protected segment encoded. */
    @MethodSource("data")
    @ParameterizedTest
    void fullyEncodedSegment_isBlocked(String reqType) {
        assertProtected(reqType, "/%61%64%6d%69%6e/secret");
    }

    /** Uppercase hex digits must decode the same as lowercase. */
    @MethodSource("data")
    @ParameterizedTest
    void uppercaseHexEncoding_isBlocked(String reqType) {
        assertProtected(reqType, "/%41dmin/secret");
    }

    /**
     * A dot-segment detour that resolves back into the protected path, and which needs no encoding at all. Spring
     * normalizes this to /admin/secret when routing, so filter selection has to see it the same way.
     */
    @MethodSource("data")
    @ParameterizedTest
    void dotSegmentTraversal_isBlocked(String reqType) {
        assertProtected(reqType, "/public/../admin/secret");
    }

    /**
     * Spellings that step OUT of the protected prefix. The canonical form is "/public/info", but Spring MVC matches
     * on the undecoded request URI and leaves dot segments to a servlet container that does not exist here, so
     * "/admin/**" still reaches an admin handler. Filter selection has to apply for the decoded-but-not-normalized
     * spelling too, since matching on the canonical path alone leaves this reaching the handler unfiltered.
     */
    @MethodSource("data")
    @ParameterizedTest
    void dotSegmentSteppingOutOfProtectedPrefix_isBlocked(String reqType) {
        assertProtected(reqType, "/admin/../public/info");
        assertProtected(reqType, "/admin/x/../../public/info");
    }

    /** The same, with the separators and dot segments percent-encoded. */
    @MethodSource("data")
    @ParameterizedTest
    void encodedSeparatorSteppingOutOfProtectedPrefix_isBlocked(String reqType) {
        assertProtected(reqType, "/admin/secret%2F..%2F..%2Fpublic%2Finfo");
    }

    /** No spelling may reach the wildcard admin handler without its filter. */
    @MethodSource("data")
    @ParameterizedTest
    void wildcardAdminHandlerIsNeverReachedUnfiltered(String reqType) {
        for (String path : new String[]{
                "/admin/../public/info",
                "/admin/x/../../public/info",
                "/admin/secret%2F..%2F..%2Fpublic%2Finfo",
                "/%61dmin/../public/info"}) {
            AwsProxyResponse resp = get(reqType, path);
            assertNotEquals(AdminWildcardController.WILDCARD_SECRET, resp.getBody(),
                    "the wildcard admin handler was reached unfiltered via " + path + " on " + reqType);
        }
    }


    /** The protected body must never be returned for any spelling of the protected path. */
    @MethodSource("data")
    @ParameterizedTest
    void noSpellingOfProtectedPathLeaksTheSecret(String reqType) {
        for (String path : new String[]{
                "/admin/secret",
                "/%61dmin/secret",
                "/adm%69n/secret",
                "/%61%64%6d%69%6e/secret",
                "/%41dmin/secret",
                "/public/../admin/secret",
                "/admin%2Fsecret",
                "/ADMIN/secret",
                "/admin//secret",
                "/./admin/secret",
                "/admin;x=1/secret",
                "/admin;/secret"
        }) {
            AwsProxyResponse resp = get(reqType, path);
            assertNotEquals(FilterAuthApplication.SECRET_BODY, resp.getBody(),
                    "the secret leaked via " + path + " on " + reqType);
        }
    }
}
