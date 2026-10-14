package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.internal.testutils.MockServlet;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.FilterRegistration;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Regression tests for the filter-selection authorization bypass: filter url-pattern matching used the raw,
 * undecoded path from <code>getRequestURI()</code> while servlet resolution used the decoded path from
 * <code>getPathInfo()</code>. A request for a percent-encoded equivalent of a protected path therefore failed to
 * select the filter mapped to that path but still routed to the servlet mapped to it.
 *
 * Filter selection must be performed on the same decoded, normalized path that servlet resolution uses, so that no
 * encoding of a path can cause the two to disagree.
 */
public class FilterChainManagerPathBypassTest {
    private static final Context lambdaContext = new MockLambdaContext();

    private ServletContext servletContext;
    private AwsFilterChainManager chainManager;

    @BeforeEach
    public void setUp() {
        servletContext = new AwsServletContext(null);
        FilterRegistration.Dynamic adminFilter = servletContext.addFilter("AdminFilter", new MockFilter());
        adminFilter.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/admin/*");
        servletContext.addServlet("adminServlet", new MockServlet()).addMapping("/admin/*");
        chainManager = new AwsFilterChainManager((AwsServletContext) servletContext);
    }

    private int filterCountFor(String path) {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder(path, "GET").build(), lambdaContext, null
        );
        req.setServletContext(servletContext);
        return chainManager.getFilterChain(req, null).filterCount();
    }

    private jakarta.servlet.Servlet servletForPath(String path) {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder(path, "GET").build(), lambdaContext, null
        );
        req.setServletContext(servletContext);
        return ((AwsServletContext) servletContext).getServletForPath(req.getPathInfo());
    }

    /** Control: the plain protected path selects the filter. If this fails the test setup is wrong. */
    @Test
    void filterChain_plainProtectedPath_selectsFilter() {
        assertEquals(1, filterCountFor("/admin/secret"));
    }

    /** Control: an unrelated path does not select the filter, i.e. the mapping is actually scoped. */
    @Test
    void filterChain_unrelatedPath_doesNotSelectFilter() {
        assertEquals(0, filterCountFor("/public/info"));
    }

    /**
     * The reported bypass. "/%61dmin/secret" decodes to "/admin/secret", which is what servlet resolution sees,
     * so filter selection must see it too.
     */
    @Test
    void filterChain_percentEncodedFirstCharacter_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/%61dmin/secret"));
    }

    /** Same bypass, encoding a character in the middle of the protected segment. */
    @Test
    void filterChain_percentEncodedMiddleCharacter_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/adm%69n/secret"));
    }

    /** Every character of the protected segment encoded. */
    @Test
    void filterChain_fullyEncodedSegment_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/%61%64%6d%69%6e/secret"));
    }

    /** Uppercase percent-encoding hex digits must decode identically to lowercase. */
    @Test
    void filterChain_uppercaseHexEncoding_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/%41dmin/secret"));
    }

    /**
     * An encoded path separator collapses to "/admin/secret" once decoded. Servlet resolution decodes it, so
     * filter selection must not treat it as a single opaque segment.
     */
    @Test
    void filterChain_encodedPathSeparator_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/admin%2Fsecret"));
    }

    /** A dot segment that normalizes back into the protected path must still select the filter. */
    @Test
    void filterChain_dotSegmentTraversal_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/public/../admin/secret"));
    }

    /**
     * Double encoding must decode exactly once, matching what servlet resolution does. "/%2561dmin" decodes to
     * "/%61dmin", which is not "/admin", so the filter legitimately does not apply.
     */
    @Test
    void filterChain_doubleEncoded_decodesExactlyOnce() {
        assertEquals(0, filterCountFor("/%2561dmin/secret"));
    }

    /** A malformed percent sequence must not throw; the request still has to be routed. */
    @Test
    void filterChain_malformedEncoding_doesNotThrow() {
        filterCountFor("/%zz/secret");
        filterCountFor("/admin%");
        filterCountFor("/admin%2");
    }

    /**
     * The matcher lowercases both sides on its exact-equality fast path but compares segments case-sensitively,
     * so the two halves of the same method disagreed. Matching is case-insensitive throughout, which is the
     * fail-safe direction: a filter may run when it need not, never the reverse.
     */
    @Test
    void filterChain_mixedCasePath_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/ADMIN/secret"));
        assertEquals(1, filterCountFor("/Admin/secret"));
    }

    /** The cache must not let an encoded request poison or satisfy the entry for a different decoded path. */
    @Test
    void filterChain_cacheKeyedOnCanonicalPath_encodedAndPlainAgree() {
        assertEquals(1, filterCountFor("/%61dmin/secret"));
        assertEquals(1, filterCountFor("/admin/secret"));
        assertEquals(0, filterCountFor("/public/info"));
        assertEquals(1, filterCountFor("/admin/secret"));
    }

    /**
     * A dot segment that steps OUT of the protected prefix must still select that prefix's filter. The canonical
     * form is "/public", but Spring MVC matches on the undecoded request URI and leaves dot segments to a servlet
     * container that does not exist here, so "/admin/**" still reaches an admin handler. Selecting the filter for
     * the decoded-but-not-normalized spelling as well is what stops that from being a bypass: matching on the
     * canonical path alone leaves this request reaching the admin handler unfiltered.
     */
    @Test
    void filterChain_dotSegmentLeadingOutOfProtectedPath_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/admin/x/../../public"));
    }

    /** Same, with the dot segments percent-encoded. */
    @Test
    void filterChain_encodedDotSegmentLeadingOut_stillSelectsFilter() {
        assertEquals(1, filterCountFor("/admin/x/%2e%2e/%2e%2e/public"));
        assertEquals(1, filterCountFor("/admin/secret%2F..%2F..%2Fpublic"));
    }

    /**
     * The mirror case: a dot segment that lands ON a protected path must select that path's filter. This is the
     * direction a decode-only canonicalization would miss.
     */
    @Test
    void filterChain_dotSegmentLeadingIntoProtectedPath_selectsFilter() {
        assertEquals(1, filterCountFor("/public/x/../../admin/secret"));
        assertNotNull(servletForPath("/public/x/../../admin/secret"));
    }

    /**
     * The invariant that matters: filter selection must never UNDER-select. If any spelling of a path could reach a
     * servlet mapped under a protected prefix, the filter for that prefix has to run. Over-selection is permitted
     * and expected, because consumers downstream disagree about which form of the path they route on, so predicting
     * a single winner is what produced this bug class in the first place.
     */
    @Test
    void filterSelectionNeverUnderSelects() {
        for (String path : new String[]{
                "/admin/secret",
                "/%61dmin/secret",
                "/adm%69n/secret",
                "/ADMIN/secret",
                "/admin%2Fsecret",
                "/admin/x/../../public",
                "/admin/x/%2e%2e/%2e%2e/public",
                "/admin/secret%2F..%2F..%2Fpublic",
                "/%61dmin/../public",
                "/public/x/../../admin/secret",
                "/admin//secret",
                "/./admin/secret",
                "/../../admin/secret",
                "/admin;x=1/secret",
                "/admin;/secret"
        }) {
            assertTrue(filterCountFor(path) > 0,
                    "no filter selected for a spelling that can reach the protected prefix: " + path);
        }
    }

    /** Unlike decodePath, canonicalizePath resolves dot segments, which is why the validator cannot use it. */
    @Test
    void canonicalizePath_resolvesDotSegments() {
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/%2e%2e/admin/secret"));
    }

    /** Path parameters are removed from every segment, and nothing else about the path changes. */
    @Test
    void stripPathParameters_removesParametersFromEachSegment() {
        assertEquals("/admin/secret", AwsHttpServletRequest.stripPathParameters("/admin;x=1/secret"));
        assertEquals("/admin/secret", AwsHttpServletRequest.stripPathParameters("/admin;/secret"));
        assertEquals("/a/b", AwsHttpServletRequest.stripPathParameters("/a;x=1;y=2/b;z"));
        assertEquals("/a/b/", AwsHttpServletRequest.stripPathParameters("/a/b;x/"));
        assertEquals("/admin/secret", AwsHttpServletRequest.stripPathParameters("/admin/secret"));
        assertNull(AwsHttpServletRequest.stripPathParameters(null));
    }

    /** A path with no relationship to the protected prefix under any spelling must not select its filter. */
    @Test
    void filterSelection_unrelatedPathsAreNotOverSelected() {
        assertEquals(0, filterCountFor("/public/info"));
        assertEquals(0, filterCountFor("/public/x/y/z"));
        assertEquals(0, filterCountFor("/"));
    }


    /**
     * AwsHttpServletRequestWrapper is used on async dispatch, and AwsProxyRequestDispatcher resolves the servlet
     * from its getPathInfo. It therefore has to report the same canonical path as the request it wraps, otherwise
     * the async path reintroduces the filter/servlet disagreement.
     */
    @Test
    void requestWrapper_reportsTheSameCanonicalPathAsTheWrappedRequest() {
        for (String path : new String[]{
                "/admin/secret", "/%61dmin/secret", "/admin/x/../../public", "/admin//secret", "/./admin/secret"}) {
            AwsProxyHttpServletRequest original = new AwsProxyHttpServletRequest(
                    new AwsProxyRequestBuilder("/unrelated", "GET").build(), lambdaContext, null);
            original.setServletContext(servletContext);
            AwsHttpServletRequestWrapper wrapped = new AwsHttpServletRequestWrapper(original, path);
            assertEquals(AwsHttpServletRequest.canonicalizePath(path), wrapped.getPathInfo(),
                    "wrapper disagrees with canonicalizePath for " + path);
        }
    }


    /** The canonicalization helper itself, independent of request plumbing. */
    @Test
    void canonicalize_decodesAndNormalizes() {
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/%61dmin/secret"));
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/admin%2Fsecret"));
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/public/../admin/secret"));
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/admin//secret"));
        assertEquals("/admin/secret", AwsHttpServletRequest.canonicalizePath("/./admin/secret"));
        assertEquals("/%61dmin/secret", AwsHttpServletRequest.canonicalizePath("/%2561dmin/secret"));
    }

    /**
     * URLDecoder.decode applies form encoding, which turns "+" into a space. Path segments must not be decoded
     * that way, so a literal "+" has to survive canonicalization.
     */
    @Test
    void canonicalize_plusIsNotTreatedAsSpace() {
        assertEquals("/admin+user/secret", AwsHttpServletRequest.canonicalizePath("/admin+user/secret"));
        assertEquals("/admin user/secret", AwsHttpServletRequest.canonicalizePath("/admin%20user/secret"));
    }

    /**
     * Characters outside the BMP are stored as surrogate pairs. Encoding each half separately replaces the
     * character with "??", and because getPathInfo() returns the canonical path the corruption would reach the
     * application. Only triggered when the path also contains a '%', since otherwise decoding is skipped.
     */
    @Test
    void canonicalize_nonBmpCharactersSurviveDecoding() {
        assertEquals("/\uD83D\uDE00/admin", AwsHttpServletRequest.canonicalizePath("/\uD83D\uDE00/%61dmin"));
        assertEquals("/\uD83D\uDE00/admin", AwsHttpServletRequest.canonicalizePath("/\uD83D\uDE00/admin"));
        // CJK Extension B, also outside the BMP
        assertEquals("/\uD840\uDC0B/admin", AwsHttpServletRequest.canonicalizePath("/\uD840\uDC0B/%61dmin"));
    }

    /** BMP characters were never affected, but pin them so the surrogate handling cannot regress them. */
    @Test
    void canonicalize_bmpMultiByteCharactersSurviveDecoding() {
        assertEquals("/caf\u00e9/admin", AwsHttpServletRequest.canonicalizePath("/caf\u00e9/%61dmin"));
        assertEquals("/\u4f60\u597d/admin", AwsHttpServletRequest.canonicalizePath("/\u4f60\u597d/%61dmin"));
    }

    /** An unpaired surrogate is malformed input and must not throw. */
    @Test
    void canonicalize_loneSurrogateDoesNotThrow() {
        AwsHttpServletRequest.canonicalizePath("/\uD83D/%61dmin");
        AwsHttpServletRequest.canonicalizePath("/%61dmin/\uD83D");
        AwsHttpServletRequest.canonicalizePath("/\uDE00/%61dmin");
    }


    /** Canonicalization must never escape the root via excess dot segments. */
    @Test
    void canonicalize_traversalAboveRootIsContained() {
        assertTrue(AwsHttpServletRequest.canonicalizePath("/../../admin/secret").startsWith("/admin"));
        assertEquals("/", AwsHttpServletRequest.canonicalizePath("/../.."));
    }

    private static class MockFilter implements Filter {
        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
        }

        @Override
        public void doFilter(ServletRequest req, ServletResponse res, FilterChain chain)
                throws IOException, ServletException {
            chain.doFilter(req, res);
        }

        @Override
        public void destroy() {
        }
    }
}
