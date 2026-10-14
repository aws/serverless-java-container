package com.amazonaws.serverless.proxy.internal.servlet;

import org.junit.jupiter.api.Test;

import javax.servlet.ServletRegistration;
import javax.servlet.http.HttpServlet;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * Regression tests for <code>AwsServletContext.getServletForPath</code>.
 *
 * The matching loop was bounded by the length of the <em>mapping</em> while indexing into the <em>request path</em>,
 * so any request with fewer segments than a registered mapping read past the end of the path array and threw
 * <code>ArrayIndexOutOfBoundsException</code>. The method is reached on every request through
 * <code>SpringBootLambdaContainerHandler</code>, <code>SpringLambdaContainerHandler</code> and
 * <code>AwsProxyRequestDispatcher</code>, and its argument is <code>getPathInfo()</code>, which is null whenever the
 * servlet path covered the whole request.
 */
public class AwsServletContextServletForPathTest {

    private static class NamedServlet extends HttpServlet {
        private final String id;

        NamedServlet(String id) {
            this.id = id;
        }

        String getId() {
            return id;
        }
    }

    private AwsServletContext contextWithMapping(String... mappings) {
        AwsServletContext ctx = new AwsServletContext(null);
        ServletRegistration.Dynamic reg = ctx.addServlet("deep", new NamedServlet("deep"));
        reg.addMapping(mappings);
        return ctx;
    }

    /** The crash: request path shorter than the mapping. Previously ArrayIndexOutOfBoundsException. */
    @Test
    void getServletForPath_pathShorterThanMapping_doesNotThrow() {
        AwsServletContext ctx = contextWithMapping("/first/second/third");
        assertDoesNotThrow(() -> ctx.getServletForPath("/first"));
        assertNull(ctx.getServletForPath("/first"));
    }

    /** Same crash one segment further in, to show it is not an off-by-one at a single depth. */
    @Test
    void getServletForPath_pathShorterThanMappingByOne_doesNotThrow() {
        AwsServletContext ctx = contextWithMapping("/first/second/third");
        assertDoesNotThrow(() -> ctx.getServletForPath("/first/second"));
        assertNull(ctx.getServletForPath("/first/second"));
    }

    /** A null path is legitimate - getPathInfo() returns null when the servlet path covered everything. */
    @Test
    void getServletForPath_nullPath_doesNotThrow() {
        AwsServletContext ctx = contextWithMapping("/first/second");
        assertDoesNotThrow(() -> ctx.getServletForPath(null));
        assertNull(ctx.getServletForPath(null));
    }

    /** A null path must still reach a catch-all servlet, exactly as "/" does. */
    @Test
    void getServletForPath_nullPathWithCatchAllMapping_returnsServlet() {
        AwsServletContext ctx = new AwsServletContext(null);
        NamedServlet root = new NamedServlet("root");
        ctx.addServlet("root", root).addMapping("/*");
        assertEquals(root, ctx.getServletForPath(null));
        assertEquals(root, ctx.getServletForPath("/"));
    }

    /** Per the servlet spec "/first/*" matches "/first" itself, not only its children. */
    @Test
    void getServletForPath_wildcardMatchesBareMappingPrefix() {
        AwsServletContext ctx = contextWithMapping("/first/*");
        assertEquals("deep", ((NamedServlet) ctx.getServletForPath("/first")).getId());
        assertEquals("deep", ((NamedServlet) ctx.getServletForPath("/first/second")).getId());
    }

    /** Existing prefix behaviour must be unchanged: a mapping matches deeper paths under it. */
    @Test
    void getServletForPath_longerPathStillMatches() {
        AwsServletContext ctx = contextWithMapping("/first");
        assertEquals("deep", ((NamedServlet) ctx.getServletForPath("/first")).getId());
        assertEquals("deep", ((NamedServlet) ctx.getServletForPath("/first/second/third")).getId());
    }

    /** An unrelated path must still miss. */
    @Test
    void getServletForPath_unrelatedPath_returnsNull() {
        AwsServletContext ctx = contextWithMapping("/first/second");
        assertNull(ctx.getServletForPath("/other"));
        assertNull(ctx.getServletForPath("/other/second"));
    }

    /** Empty path keeps its existing behaviour of not matching a scoped mapping. */
    @Test
    void getServletForPath_emptyPath_returnsNull() {
        AwsServletContext ctx = contextWithMapping("/first/second");
        assertNull(ctx.getServletForPath(""));
    }
}
