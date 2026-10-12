package com.amazonaws.serverless.proxy.internal.servlet.filters;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * UrlPathValidator's ratio check rejects traversal above the root, such as "/../x". It is a heuristic, not a general
 * traversal guard, and these cases stay within the range it covers. It needs the path decoded but not normalized: the
 * raw URI leaves "%2e%2e" unrecognizable as "..", and the canonical path has already resolved the dot segments away.
 */
public class UrlPathValidatorTraversalTest {

    @AfterEach
    public void reset() {
        LambdaContainerHandler.getContainerConfig().setServiceBasePath(null);
        LambdaContainerHandler.getContainerConfig().setUseStageAsServletContext(false);
    }

    private int statusFor(String path) {
        AwsProxyHttpServletRequest req =
                new AwsProxyHttpServletRequest(new AwsProxyRequestBuilder(path, "GET").build(), null, null);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(req, null);
        UrlPathValidator validator = new UrlPathValidator();
        final boolean[] chainCalled = {false};
        try {
            validator.init(null);
            validator.doFilter(req, resp, (rq, rs) -> chainCalled[0] = true);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return resp.getStatus();
    }

    /** Plain traversal was always rejected. */
    @Test
    void plainTraversal_isRejected() {
        assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("../.."));
    }

    /** Percent-encoded traversal must be rejected too. Reading the raw URI misses this entirely. */
    @Test
    void percentEncodedTraversal_isRejected() {
        assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("/%2e%2e/%2e%2e/x"));
    }

    /** Uppercase escapes decode the same way. */
    @Test
    void uppercaseEncodedTraversal_isRejected() {
        assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("/%2E%2E/%2E%2E/x"));
    }

    /** Mixed plain and encoded dots form a traversal too. */
    @Test
    void mixedEncodingTraversal_isRejected() {
        assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("/.%2e/.%2e/x"));
    }

    /**
     * A configured base path contributes slashes but never dot segments, which loosened the ratio check. The
     * decision must not depend on how the context path is configured.
     */
    @Test
    void configuredBasePath_doesNotWeakenTheCheck() {
        int withoutContext = statusFor("/../x");
        LambdaContainerHandler.getContainerConfig().setServiceBasePath("/prod");
        int withContext = statusFor("/../x");
        assertEquals(withoutContext, withContext,
                "the verdict changed when a context path was configured");
    }

    /** An ordinary path must still be allowed through. */
    @Test
    void ordinaryPath_isAllowed() {
        assertNotEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("/admin/secret"));
        assertNotEquals(UrlPathValidator.DEFAULT_ERROR_CODE, statusFor("/admin/a.b/secret"));
    }

    /** The decode-only helper leaves dot segments in place. */
    @Test
    void decodePath_leavesDotSegmentsInPlace() {
        assertEquals("/../../x", AwsHttpServletRequest.decodePath("/%2e%2e/%2e%2e/x"));
    }
}
