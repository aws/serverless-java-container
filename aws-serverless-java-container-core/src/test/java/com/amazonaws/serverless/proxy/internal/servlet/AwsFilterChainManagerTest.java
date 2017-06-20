package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.services.lambda.runtime.Context;
import org.junit.BeforeClass;
import org.junit.Test;

import javax.servlet.*;

import java.io.IOException;
import java.util.EnumSet;

import static org.junit.Assert.*;

public class AwsFilterChainManagerTest {

    private static AwsFilterChainManager chainManager;
    private static Context lambdaContext = new MockLambdaContext();
    private static ServletContext servletContext;

    @BeforeClass
    public static void setUp() {
        servletContext = new AwsServletContext(lambdaContext, null);//AwsServletContext.getInstance(lambdaContext, null);

        FilterRegistration.Dynamic reg = servletContext.addFilter("Filter1", new MockFilter());
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/first/second");
        FilterRegistration.Dynamic reg2 = servletContext.addFilter("Filter2", new MockFilter());
        reg2.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/second/*");
        FilterRegistration.Dynamic reg3 = servletContext.addFilter("Filter3", new MockFilter());
        reg3.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/third/fourth/*");

        chainManager = new AwsFilterChainManager((AwsServletContext) servletContext);
    }

    @Test
    public void paths_pathMatches_validPaths() {
        assertTrue(chainManager.pathMatches("/users/123123123", "/users/*"));
        assertTrue(chainManager.pathMatches("/apis/123/methods", "/apis/*"));
        assertTrue(chainManager.pathMatches("/very/long/path/with/sub/resources", "/*"));

        assertFalse(chainManager.pathMatches("/false/api", "/true/*"));
        assertFalse(chainManager.pathMatches("/first/second/third", "/first/third/*"));
        assertFalse(chainManager.pathMatches("/first/second/third", "/first/third/second"));
    }

    @Test
    public void paths_pathMatches_invalidPaths() {
        // I expect we'd want to run filters on these requests, especially the ones that look invalid
        assertTrue(chainManager.pathMatches("_%Garbled%20Path_%", "/*"));
        assertTrue(chainManager.pathMatches("<script>alert('message');</script>", "/*"));

        assertFalse(chainManager.pathMatches("<script>alert('message');</script>", "/test/*"));
    }

    @Test
    public void cacheKey_compare_samePath() {
        FilterChainManager.TargetCacheKey cacheKey = new FilterChainManager.TargetCacheKey();
        cacheKey.setDispatcherType(DispatcherType.REQUEST);
        cacheKey.setTargetPath("/first/path");

        FilterChainManager.TargetCacheKey secondCacheKey = new FilterChainManager.TargetCacheKey();
        secondCacheKey.setDispatcherType(DispatcherType.REQUEST);
        secondCacheKey.setTargetPath("/first/path");

        assertEquals(cacheKey.hashCode(), secondCacheKey.hashCode());
        assertTrue(cacheKey.equals(secondCacheKey));
    }

    @Test
    public void cacheKey_compare_differentDispatcher() {
        FilterChainManager.TargetCacheKey cacheKey = new FilterChainManager.TargetCacheKey();
        cacheKey.setDispatcherType(DispatcherType.REQUEST);
        cacheKey.setTargetPath("/first/path");

        FilterChainManager.TargetCacheKey secondCacheKey = new FilterChainManager.TargetCacheKey();
        secondCacheKey.setDispatcherType(DispatcherType.ASYNC);
        secondCacheKey.setTargetPath("/first/path");

        assertNotEquals(cacheKey.hashCode(), secondCacheKey.hashCode());
        assertFalse(cacheKey.equals(secondCacheKey));
    }

    @Test
    public void cacheKey_compare_additionalChars() {
        FilterChainManager.TargetCacheKey cacheKey = new FilterChainManager.TargetCacheKey();
        cacheKey.setDispatcherType(DispatcherType.REQUEST);
        cacheKey.setTargetPath("/first/path");

        FilterChainManager.TargetCacheKey secondCacheKey = new FilterChainManager.TargetCacheKey();
        secondCacheKey.setDispatcherType(DispatcherType.REQUEST);
        secondCacheKey.setTargetPath("/first/path/");
        assertEquals(cacheKey.hashCode(), secondCacheKey.hashCode());
        assertTrue(cacheKey.equals(secondCacheKey));

        secondCacheKey.setTargetPath(" /first/path");
        assertEquals(cacheKey.hashCode(), secondCacheKey.hashCode());
        assertTrue(cacheKey.equals(secondCacheKey));

        secondCacheKey.setTargetPath("first/path/");
        assertEquals(cacheKey.hashCode(), secondCacheKey.hashCode());
        assertTrue(cacheKey.equals(secondCacheKey));
    }

    @Test
    public void filterChain_getFilterChain_subsetOfFilters() {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(
            new AwsProxyRequestBuilder("/first/second", "GET").build(), lambdaContext, null
        );
        req.setServletContext(servletContext);
        FilterChainHolder fcHolder = chainManager.getFilterChain(req);
        assertEquals(1, fcHolder.filterCount());
        assertEquals("Filter1", fcHolder.getFilter(0).getFilterName());

        req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder("/second/mime", "GET").build(), lambdaContext, null
        );
        fcHolder = chainManager.getFilterChain(req);
        assertEquals(1, fcHolder.filterCount());
        assertEquals("Filter2", fcHolder.getFilter(0).getFilterName());

        req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder("/second/mime/third", "GET").build(), lambdaContext, null
        );
        fcHolder = chainManager.getFilterChain(req);
        assertEquals(1, fcHolder.filterCount());
        assertEquals("Filter2", fcHolder.getFilter(0).getFilterName());
    }

    @Test
    public void filterChain_getFilterChain_multipleFilters() {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(
                new AwsProxyRequestBuilder("/second/important", "GET").build(), lambdaContext, null
        );
        req.setServletContext(servletContext);
        FilterRegistration.Dynamic reg = req.getServletContext().addFilter("Filter4", new MockFilter());
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/second/*");
        FilterChainHolder fcHolder = chainManager.getFilterChain(req);
        assertEquals(2, fcHolder.filterCount());
        assertEquals("Filter2", fcHolder.getFilter(0).getFilterName());
        assertEquals("Filter4", fcHolder.getFilter(1).getFilterName());

        reg = req.getServletContext().addFilter("Filter5", new MockFilter());
        reg.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), false, "/second/*");
        fcHolder = chainManager.getFilterChain(req);
        assertEquals(3, fcHolder.filterCount());
        assertEquals("Filter2", fcHolder.getFilter(0).getFilterName());
        assertEquals("Filter4", fcHolder.getFilter(1).getFilterName());
        assertEquals("Filter5", fcHolder.getFilter(2).getFilterName());
    }

    private static class MockFilter implements Filter {

        @Override
        public void init(FilterConfig filterConfig) throws ServletException {
            System.out.println("Init");
        }

        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain) throws IOException, ServletException {
            System.out.println("DoFilter");
            filterChain.doFilter(servletRequest, servletResponse);
        }

        @Override
        public void destroy() {
            System.out.println("Destroy");
        }
    }
}
