/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.jersey;

import com.amazonaws.serverless.proxy.AwsHttpApiV2SecurityContextWriter;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpApiV2HttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletRegistration;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import jakarta.servlet.Filter;
import jakarta.servlet.FilterChain;
import jakarta.servlet.Servlet;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.Context;
import jakarta.ws.rs.core.MediaType;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Collection;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.stream.Stream;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * Servlet filters registered on a Jersey handler must run before the JAX-RS resource, as they would in front of a
 * servlet in a servlet container. Filters added through <code>onStartup</code> after one of the default factory
 * methods used to run only after Jersey had already executed the resource and written its response, so a filter that
 * denied the request could not stop it.
 *
 * Requests go through <code>proxyStream</code>, the entry point a Lambda function's stream handler calls, which
 * deserializes the event and hands it to <code>proxy</code>.
 */
public class JerseyFilterChainTest {
    private static final String SECRET_BODY = "TOP_SECRET";
    private static final String FORBIDDEN_BODY = "FORBIDDEN";
    private static final String FILTER_ATTRIBUTE = "com.amazonaws.serverless.jersey.test.filterRan";
    private static final String NO_ATTRIBUTE = "NO_ATTRIBUTE";
    private static final String FILTER_HEADER = "X-Filter-Header";

    private static final AtomicInteger RESOURCE_INVOCATIONS = new AtomicInteger(0);

    private final MockLambdaContext lambdaContext = new MockLambdaContext();

    public static Collection<Object> data() {
        return Arrays.asList(new Object[]{"API_GW", "ALB", "HTTP_API"});
    }

    @BeforeEach
    void resetInvocations() {
        RESOURCE_INVOCATIONS.set(0);
    }

    @MethodSource("data")
    @ParameterizedTest
    void denyingFilter_registeredAfterFactory_blocksResource(String reqType) {
        AwsProxyResponse resp = get(reqType, "/filter-chain/admin/secret",
                ctx -> ctx.addFilter("deny", new DenyFilter()).addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(403, resp.getStatusCode());
        assertEquals(FORBIDDEN_BODY, resp.getBody());
        assertEquals(0, RESOURCE_INVOCATIONS.get());
    }

    /** Registering filters between the constructor and <code>initialize()</code> must keep working. */
    @MethodSource("data")
    @ParameterizedTest
    void denyingFilter_registeredBeforeInitialize_blocksResource(String reqType) {
        AwsProxyResponse resp = getRegisteredBeforeInitialize(reqType, "/filter-chain/admin/secret",
                ctx -> ctx.addFilter("deny", new DenyFilter()).addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(403, resp.getStatusCode());
        assertEquals(FORBIDDEN_BODY, resp.getBody());
        assertEquals(0, RESOURCE_INVOCATIONS.get());
    }

    @MethodSource("data")
    @ParameterizedTest
    void denyingFilter_scopedToProtectedPath_leavesOtherPathsServed(String reqType) {
        Consumer<ServletContext> startup = ctx -> ctx.addFilter("deny", new DenyFilter())
                .addMappingForUrlPatterns(null, true, "/filter-chain/admin/*");

        AwsProxyResponse denied = get(reqType, "/filter-chain/admin/secret", startup);
        assertEquals(403, denied.getStatusCode());
        assertEquals(FORBIDDEN_BODY, denied.getBody());
        assertEquals(0, RESOURCE_INVOCATIONS.get());

        AwsProxyResponse served = get(reqType, "/filter-chain/public/attribute", startup);
        assertEquals(200, served.getStatusCode());
        assertEquals(NO_ATTRIBUTE, served.getBody());
    }

    /**
     * Other ways of writing a path under /filter-chain/admin. With no filters, Jersey serves the protected resources
     * for the doubled slashes, the matrix parameters, the trailing slash, the dot segments after /admin, and the
     * encoded "?"; the rest currently 404. All of them are kept so the test still holds if Jersey's routing of any
     * spelling changes.
     */
    public static Stream<Arguments> protectedPathSpellings() {
        String[] paths = {
                "/filter-chain/%61dmin/secret",
                "/filter-chain/adm%69n/secret",
                "/filter-chain/public/../admin/secret",
                "/filter-chain/./admin/secret",
                "/filter-chain//admin/secret",
                "/filter-chain/admin;x=1/secret",
                "/filter-chain/admin/secret;x=1",
                "/filter-chain/ADMIN/secret",
                "/FILTER-CHAIN/admin/secret",
                "/filter-chain/admin/secret/",
                "/filter-chain/admin%2fsecret",
                "/filter-chain/public/%2e%2e/admin/secret",
                "/filter-chain/admin/../public/attribute",
                "/filter-chain/admin;/secret",
                "/filter-chain/admin%3Bx=1/secret",
                "/filter-chain/admin/;x=1/secret",
                "/filter-chain/admin//secret",
                "/filter-chain/admin/%2e/secret",
                "/filter-chain/admin/x/../secret",
                "/filter-chain/admin/secret%3fx=1",
                "/filter-chain/;x=1/admin/secret",
                "/filter-chain/..;/admin/secret",
        };
        return data().stream().flatMap(type -> Arrays.stream(paths).map(path -> Arguments.of(type, path)));
    }

    @MethodSource("protectedPathSpellings")
    @ParameterizedTest
    void scopedDenyingFilter_otherSpellingOfProtectedPath_resourceNotReached(String reqType, String path) {
        AwsProxyResponse resp = get(reqType, path, ctx -> ctx.addFilter("deny", new DenyFilter())
                .addMappingForUrlPatterns(null, true, "/filter-chain/admin/*"));

        assertEquals(0, RESOURCE_INVOCATIONS.get(),
                path + " on " + reqType + " reached the resource: " + resp.getStatusCode() + " " + resp.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void passThroughFilter_registeredAfterFactory_runsBeforeResource(String reqType) {
        AwsProxyResponse resp = get(reqType, "/filter-chain/public/attribute",
                ctx -> ctx.addFilter("attribute", new AttributeFilter("filter"))
                        .addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(200, resp.getStatusCode());
        assertEquals("filter", resp.getBody());
        assertEquals(1, RESOURCE_INVOCATIONS.get());
    }

    /** Filters run in the order they were registered, whether that was before or after <code>initialize()</code>. */
    @MethodSource("data")
    @ParameterizedTest
    void filtersRegisteredBeforeAndAfterInitialize_runInRegistrationOrder(String reqType) {
        JerseyLambdaContainerHandler<?, AwsProxyResponse> handler = uninitializedHandler(reqType);
        handler.onStartup(ctx -> ctx.addFilter("first", new AttributeFilter("first"))
                .addMappingForUrlPatterns(null, true, "/*"));
        handler.initialize();
        handler.onStartup(ctx -> ctx.addFilter("second", new AttributeFilter("second"))
                .addMappingForUrlPatterns(null, true, "/*"));

        AwsProxyResponse resp = proxyStream(reqType, handler, "/filter-chain/public/attribute");

        assertEquals(200, resp.getStatusCode());
        assertEquals("first,second", resp.getBody());
    }

    @MethodSource("data")
    @ParameterizedTest
    void headerFilter_registeredAfterFactory_headerInResponse(String reqType) {
        AwsProxyResponse resp = get(reqType, "/filter-chain/admin/secret",
                ctx -> ctx.addFilter("header", new HeaderFilter()).addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(200, resp.getStatusCode());
        assertEquals(SECRET_BODY, resp.getBody());
        assertEquals("ran", resp.getMultiValueHeaders().getFirst(FILTER_HEADER));
    }

    @MethodSource("data")
    @ParameterizedTest
    void forwardingFilter_registeredAfterFactory_forwardedPathServedByJersey(String reqType) {
        AwsProxyResponse resp = get(reqType, "/filter-chain/forward",
                ctx -> ctx.addFilter("forward", new ForwardFilter()).addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(200, resp.getStatusCode());
        assertEquals(NO_ATTRIBUTE, resp.getBody());
        assertEquals(1, RESOURCE_INVOCATIONS.get());
    }

    @MethodSource("data")
    @ParameterizedTest
    void forwardingFilter_registeredBeforeInitialize_forwardedPathServedByJersey(String reqType) {
        AwsProxyResponse resp = getRegisteredBeforeInitialize(reqType, "/filter-chain/forward",
                ctx -> ctx.addFilter("forward", new ForwardFilter()).addMappingForUrlPatterns(null, true, "/*"));

        assertEquals(200, resp.getStatusCode());
        assertEquals(NO_ATTRIBUTE, resp.getBody());
        assertEquals(1, RESOURCE_INVOCATIONS.get());
    }

    /**
     * A second <code>initialize()</code> call, which a handler built with the constructor and then passed to code
     * that initializes defensively can produce. It used to throw, because the duplicate filter registration returned
     * null.
     */
    @MethodSource("data")
    @ParameterizedTest
    void initializeCalledTwice_requestStillServed(String reqType) {
        JerseyLambdaContainerHandler<?, AwsProxyResponse> handler = "HTTP_API".equals(reqType)
                ? JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(newApp())
                : JerseyLambdaContainerHandler.getAwsProxyHandler(newApp());
        handler.initialize();

        AwsProxyResponse resp = proxyStream(reqType, handler, "/filter-chain/admin/secret");

        assertEquals(200, resp.getStatusCode());
        assertEquals(SECRET_BODY, resp.getBody());
    }

    /** The handler hands back the servlet it routes to, which is the one registered on the servlet context. */
    @MethodSource("data")
    @ParameterizedTest
    void getServlet_returnsTheServletRegisteredOnTheContext(String reqType) {
        JerseyLambdaContainerHandler<?, AwsProxyResponse> handler = "HTTP_API".equals(reqType)
                ? JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(newApp())
                : JerseyLambdaContainerHandler.getAwsProxyHandler(newApp());

        Servlet registered = ((AwsServletRegistration) handler.getServletContext()
                .getServletRegistrations().values().iterator().next()).getServlet();

        assertNotNull(handler.getServlet());
        assertSame(registered, handler.getServlet());
    }

    @MethodSource("data")
    @ParameterizedTest
    void noFilters_resourceIsServed(String reqType) {
        AwsProxyResponse resp = get(reqType, "/filter-chain/admin/secret", ctx -> { });

        assertEquals(200, resp.getStatusCode());
        assertEquals(SECRET_BODY, resp.getBody());
        assertEquals(1, RESOURCE_INVOCATIONS.get());
    }

    // registers filters on a handler from one of the default factory methods, which initialize it before returning
    private AwsProxyResponse get(String reqType, String path, Consumer<ServletContext> startup) {
        JerseyLambdaContainerHandler<?, AwsProxyResponse> handler = "HTTP_API".equals(reqType)
                ? JerseyLambdaContainerHandler.getHttpApiV2ProxyHandler(newApp())
                : JerseyLambdaContainerHandler.getAwsProxyHandler(newApp());
        handler.onStartup(startup::accept);
        return proxyStream(reqType, handler, path);
    }

    private AwsProxyResponse getRegisteredBeforeInitialize(String reqType, String path, Consumer<ServletContext> startup) {
        JerseyLambdaContainerHandler<?, AwsProxyResponse> handler = uninitializedHandler(reqType);
        handler.onStartup(startup::accept);
        handler.initialize();
        return proxyStream(reqType, handler, path);
    }

    // the same readers and writers the factory methods use, without the initialize() call
    private static JerseyLambdaContainerHandler<?, AwsProxyResponse> uninitializedHandler(String reqType) {
        if ("HTTP_API".equals(reqType)) {
            return new JerseyLambdaContainerHandler<>(
                    HttpApiV2ProxyRequest.class,
                    AwsProxyResponse.class,
                    new AwsHttpApiV2HttpServletRequestReader(),
                    new AwsProxyHttpServletResponseWriter(true),
                    new AwsHttpApiV2SecurityContextWriter(),
                    new AwsProxyExceptionHandler(),
                    newApp());
        }
        return new JerseyLambdaContainerHandler<>(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler(),
                newApp());
    }

    private AwsProxyResponse proxyStream(String reqType, JerseyLambdaContainerHandler<?, AwsProxyResponse> handler,
                                         String path) {
        AwsProxyRequestBuilder request = new AwsProxyRequestBuilder(path, "GET");
        InputStream event;
        switch (reqType) {
            case "API_GW":
                event = request.buildStream();
                break;
            case "ALB":
                event = request.alb().buildStream();
                break;
            case "HTTP_API":
                event = request.toHttpApiV2RequestStream();
                break;
            default:
                throw new RuntimeException("Unknown request type: " + reqType);
        }
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        try {
            handler.proxyStream(event, output, lambdaContext);
            return LambdaContainerHandler.getObjectMapper().readValue(output.toByteArray(), AwsProxyResponse.class);
        } catch (IOException e) {
            throw new UncheckedIOException(e);
        }
    }

    // each handler locks the ResourceConfig it is given, so every handler needs its own
    private static ResourceConfig newApp() {
        return new ResourceConfig().register(FilterChainResource.class);
    }

    @Path("/filter-chain")
    public static class FilterChainResource {
        @GET
        @Path("/admin/secret")
        @Produces(MediaType.TEXT_PLAIN)
        public String secret() {
            RESOURCE_INVOCATIONS.incrementAndGet();
            return SECRET_BODY;
        }

        // a catch-all under the protected prefix, so that every spelling Jersey routes below it reaches a resource
        @GET
        @Path("/admin/{sub: .*}")
        @Produces(MediaType.TEXT_PLAIN)
        public String adminCatchAll() {
            RESOURCE_INVOCATIONS.incrementAndGet();
            return SECRET_BODY;
        }

        @GET
        @Path("/public/attribute")
        @Produces(MediaType.TEXT_PLAIN)
        public String attribute(@Context HttpServletRequest request) {
            RESOURCE_INVOCATIONS.incrementAndGet();
            Object value = request.getAttribute(FILTER_ATTRIBUTE);
            return value == null ? NO_ATTRIBUTE : value.toString();
        }
    }

    /** Denies every request it sees and never calls <code>chain.doFilter</code>. */
    private static class DenyFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException {
            HttpServletResponse httpResponse = (HttpServletResponse) response;
            httpResponse.setStatus(403);
            httpResponse.setContentType(MediaType.TEXT_PLAIN);
            httpResponse.getWriter().write(FORBIDDEN_BODY);
        }
    }

    /** Appends its name to a request attribute before passing the request on, so the resource can report who ran. */
    private static class AttributeFilter implements Filter {
        private final String name;

        AttributeFilter(String name) {
            this.name = name;
        }

        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            Object previous = request.getAttribute(FILTER_ATTRIBUTE);
            request.setAttribute(FILTER_ATTRIBUTE, previous == null ? name : previous + "," + name);
            chain.doFilter(request, response);
        }
    }

    /** Sets a response header before passing the request on, the way a CORS filter does. */
    private static class HeaderFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            ((HttpServletResponse) response).setHeader(FILTER_HEADER, "ran");
            chain.doFilter(request, response);
        }
    }

    /**
     * Forwards to the public resource instead of passing the request on. It is only mapped for REQUEST and ASYNC
     * dispatches, so it does not run again for the forward.
     */
    private static class ForwardFilter implements Filter {
        @Override
        public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain)
                throws IOException, ServletException {
            request.getRequestDispatcher("/filter-chain/public/attribute").forward(request, response);
        }
    }
}
