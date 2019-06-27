package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.filters.UrlPathValidator;

import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Test;

import javax.servlet.Filter;
import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.FilterRegistration;
import javax.servlet.Servlet;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.nio.file.Files;
import java.nio.file.Paths;

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
            fail("Could not add tmp dir to valid paths");
            e.printStackTrace();
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
            fail("Unrecognized exception");
            e.printStackTrace();
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
    public void unsupportedOprations_expectExceptions() {
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
        try {
            STATIC_CTX.getServlet("1");
        } catch (UnsupportedOperationException e) {
            exCount++;
        } catch (ServletException e) {
            fail("Unexpected exception from unimplemented method");
            e.printStackTrace();
        }
        try {
            STATIC_CTX.addServlet("1", "");
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        try {
            STATIC_CTX.addServlet("1", new Servlet() {
                @Override
                public void init(ServletConfig config)
                        throws ServletException {

                }


                @Override
                public ServletConfig getServletConfig() {
                    return null;
                }


                @Override
                public void service(ServletRequest req, ServletResponse res)
                        throws ServletException, IOException {

                }


                @Override
                public String getServletInfo() {
                    return null;
                }


                @Override
                public void destroy() {

                }
            });
        } catch (UnsupportedOperationException e) {
            exCount++;
        }
        assertEquals(5, exCount);

        assertNull(STATIC_CTX.getServletRegistration("1"));
        assertNull(STATIC_CTX.getServletRegistrations());
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
