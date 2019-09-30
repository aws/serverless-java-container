package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.filters.UrlPathValidator;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.concurrent.CountDownLatch;

import static org.junit.Assert.*;

public class AwsServletContextTest {
    private static String TMP_DIR = System.getProperty("java.io.tmpdir");
    private static final AwsServletContext STATIC_CTX = new AwsServletContext(null);

    @BeforeClass
    public static void setUp() {
        LambdaContainerHandler.getContainerConfig().addValidFilePath("/private/var/task");
        File tmpFile = new File(TMP_DIR);
        try {
            LambdaContainerHandler.getContainerConfig().addValidFilePath(tmpFile.getCanonicalPath());
        } catch (IOException e) {
            e.printStackTrace();
            fail("Could not add tmp dir to valid paths");
        }
        LambdaContainerHandler.getContainerConfig().addValidFilePath("C:\\MyTestFolder");
    }

    @Test @Ignore
    public void getMimeType_disabledPath_expectException() {
        AwsServletContext ctx = new AwsServletContext(null);
        try {
            assertNull(ctx.getMimeType("/usr/local/lib/nothing"));
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith("File path not allowed"));
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unrecognized exception");
        }
    }

    @Test
    public void getMimeType_nonExistentFileInTaskPath_expectNull() {
        AwsServletContext ctx = new AwsServletContext(null);
        assertNull(ctx.getMimeType("/var/task/nothing"));
    }

    @Test
    public void getMimeType_mimeTypeOfCorrectFile_expectMime() {
        String tmpFilePath = TMP_DIR + "test_text.txt";
        AwsServletContext ctx = new AwsServletContext(null);
        String mimeType = ctx.getMimeType(tmpFilePath);
        assertEquals("text/plain", mimeType);

        mimeType = ctx.getMimeType("file://" + tmpFilePath);
        assertEquals("text/plain", mimeType);
    }

    @Test
    public void getMimeType_unknownExtension_expectAppOctetStream() {
        AwsServletContext ctx = new AwsServletContext(null);
        String mimeType = ctx.getMimeType("myfile.unkext");
        assertEquals("application/octet-stream", mimeType);
    }


    @Test
    public void addFilter_nonExistentFilterClass_expectException() {
        AwsServletContext ctx = new AwsServletContext(null);
        String filterClass = "com.amazonaws.serverless.TestingFilterClassNonExistent";
        try {
            ctx.addFilter("filter", filterClass);
        } catch (IllegalStateException e) {
            assertTrue(e.getMessage().startsWith("Filter class " + filterClass));
            return;
        }
        fail("Expected IllegalStateException");
    }

    @Test
    public void addFilter_doesNotImplementFilter_expectException() {
        AwsServletContext ctx = new AwsServletContext(null);
        try {
            ctx.addFilter("filter", this.getClass().getName());
        } catch (IllegalArgumentException e) {
            assertTrue(e.getMessage().startsWith(this.getClass().getName() + " does not implement Filter"));
            return;
        }
        fail("Expected IllegalArgumentException");
    }

    @Test
    public void addFilter_validFilter_expectSuccess() {
        AwsServletContext ctx = new AwsServletContext(null);
        FilterRegistration.Dynamic reg = ctx.addFilter("filter", UrlPathValidator.class.getName());
        assertNotNull(reg);
        assertNotNull(ctx.getFilterHolders());
        assertEquals(1, ctx.getFilterHolders().size());
        // uses annotated filter name
        assertEquals(reg, ctx.getFilterRegistration("UrlPathValidator"));

        assertNotNull(ctx.getFilterRegistrations());
        assertEquals(1, ctx.getFilterRegistrations().size());
    }

    @Test
    public void addFilter_validFilter_expectSuccessWithCustomFilterName() {
        AwsServletContext ctx = new AwsServletContext(null);
        FilterRegistration.Dynamic reg = ctx.addFilter("filter", TestFilter.class.getName());
        assertNotNull(reg);
        assertNotNull(ctx.getFilterHolders());
        assertEquals(1, ctx.getFilterHolders().size());
        // uses annotated filter name
        assertEquals(reg, ctx.getFilterRegistration("filter"));

        assertNotNull(ctx.getFilterRegistrations());
        assertEquals(1, ctx.getFilterRegistrations().size());
    }

    @Test
    public void getContextPath_expectEmpty() {
        assertEquals("", STATIC_CTX.getContextPath());
    }

    @Test
    public void getContext_returnsSameContext() {
        assertEquals(STATIC_CTX, STATIC_CTX.getContext("1"));
        assertEquals(STATIC_CTX, STATIC_CTX.getContext("2"));
    }

    @Test
    public void getVersions_expectStaticVersions() {
        assertEquals(AwsServletContext.SERVLET_API_MAJOR_VERSION, STATIC_CTX.getMajorVersion());
        assertEquals(AwsServletContext.SERVLET_API_MINOR_VERSION, STATIC_CTX.getMinorVersion());
        assertEquals(AwsServletContext.SERVLET_API_MAJOR_VERSION, STATIC_CTX.getEffectiveMajorVersion());
        assertEquals(AwsServletContext.SERVLET_API_MINOR_VERSION, STATIC_CTX.getEffectiveMinorVersion());
    }

    @Test
    public void startAsync_expectPopulatedAsyncContext() {
        HttpServletRequest req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder("/", "GET").build(),
                null,
                null
        );
        assertNotNull(req);
        AsyncContext ctx = req.startAsync();
        assertNotNull(ctx);
        assertEquals(req, ctx.getRequest());
    }

    @Test
    public void startAsyncWithNewRequest_expectPopulatedAsyncContext() {
        HttpServletRequest req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder("/", "GET").build(),
                null,
                null
        );
        assertNotNull(req);
        HttpServletRequest newReq = new AwsHttpServletRequestWrapper(req, "/new");
        HttpServletResponse newResp = new AwsHttpServletResponse(newReq, new CountDownLatch(1));
        AsyncContext ctx = req.startAsync(newReq, newResp);
        assertNotNull(ctx);
        assertNotNull(req.getAsyncContext());
        assertNotNull(newReq.getAsyncContext());
        assertEquals(newReq, ctx.getRequest());
        assertEquals(newResp, ctx.getResponse());
    }

    @Test
    public void unsupportedOperations_expectExceptions() {
        int exCount = 0;
        try {
            STATIC_CTX.getResourcePaths("1");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        try {
            STATIC_CTX.getNamedDispatcher("1");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
       assertEquals(2, exCount);

        assertNull(STATIC_CTX.getServletRegistration("1"));
    }

    @Test
    public void servletMappings_expectCorrectServlet() {
        AwsServletContext ctx = new AwsServletContext(null);
        TestServlet srv1 = new TestServlet("srv1");
        TestServlet srv2 = new TestServlet("srv2");

        ServletRegistration.Dynamic reg1 = ctx.addServlet("srv1", srv1);
        ServletRegistration.Dynamic reg2 = ctx.addServlet("srv2", srv2);

        reg1.addMapping("/srv1");
        reg2.addMapping("/srv2");

        assertEquals(srv1, ctx.getServletForPath("/srv1/hello"));
        assertEquals(srv1, ctx.getServletForPath("/srv1/hello/test"));
        assertEquals(srv2, ctx.getServletForPath("/srv2"));
        assertEquals(srv2, ctx.getServletForPath("/srv2/hello"));
        assertNull(ctx.getServletForPath("/srv3"));
        assertNull(ctx.getServletForPath(""));

        reg2.addMapping("/");
        assertEquals(srv2, ctx.getServletForPath("/srv3"));
    }

    @Test
    public void addServlet_callsDefaultConstructor() throws ServletException {
        AwsServletContext ctx = new AwsServletContext(null);
        ctx.addServlet("srv1", TestServlet.class);
        assertNotNull(ctx.getServlet("srv1"));
        assertNotNull(ctx.getServletRegistration("srv1"));
        assertEquals("", ((TestServlet)ctx.getServlet("srv1")).getId());
    }

    public static class TestServlet implements Servlet {
        private String id;

        public TestServlet() {
            this("");
        }

        public TestServlet(String identifier) {
            id = identifier;
        }

        @Override
        public void init(ServletConfig servletConfig) throws ServletException {

        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {

        }

        public String getId() {
            return id;
        }
    }

    public static class TestFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig)
                throws ServletException {

        }


        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {

        }


        @Override
        public void destroy() {

        }
    }
}
