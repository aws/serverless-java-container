/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy.internal.servlet;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.function.Predicate;

/**
 * This object is in charge of matching a servlet request to a set of filters, creating the filter chain for a request,
 * and cache filter chains that were already loaded for re-use. This object should be used by the framework-specific
 * implementations that use the <code>HttpServletRequest</code> and <code>HttpServletResponse</code> objects.
 *
 * For example, the Spring implementation creates the ServletContext when the application is initialized the first time
 * and creates a FilterChainManager to execute its filters for each request.
 */
public abstract class FilterChainManager<ServletContextType extends ServletContext> {

    //-------------------------------------------------------------
    // Variables - Protected
    //-------------------------------------------------------------

    static final String PATH_PART_SEPARATOR = "/";


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    // we use the synchronizedMap because we do not expect high concurrency on this object. Lambda only allows one
    // event at a time per container
    private Map<TargetCacheKey, List<FilterHolder>> filterCache = Collections.synchronizedMap(new HashMap<TargetCacheKey, List<FilterHolder>>());
    private int filtersSize = -1;
    protected ServletContextType servletContext;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    FilterChainManager(ServletContextType context) {
        servletContext = context;
    }


    //-------------------------------------------------------------
    // Methods - Abstract
    //-------------------------------------------------------------

    /**
     * This method is used by the <code>getFilterChain</code> method to extract a Map of filter holders from the current
     * context. This method is implemented in the <code>AwsFilterChainManager</code> class.
     * @return A Map of filter holders with the filter name as key and the filter holder object as value
     */
    protected abstract Map<String, FilterHolder> getFilterHolders();


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Returns the filter chain that applies to the given request. The method matches requests to filters using the
     * <code>DispatcherType</code> value (set to REQUEST by default in the AwsProxyHttpServletRequest) and the target
     * path returned by the <code>getPath</code> method of the request.
     *
     * This method currently does not filter on servlet name because the library assumes we are using a single servlet.
     * @param request The incoming servlet request
     * @param servlet The final servlet in the filter chain (if any)
     * @return A <code>FilterChainHolder</code> object that can be used to apply the filters to the request
     */
    FilterChainHolder getFilterChain(final HttpServletRequest request, Servlet servlet) {
        // Filter selection and servlet resolution used to read the path from different sources, which is the bypass
        // this fixes. They cannot just be pointed at one source, because consumers downstream disagree about which
        // form they route on, so a filter applies if its url-pattern matches under any of three spellings.
        //
        // The canonical path is the one servlet resolution here uses, since getPathInfo() is decoded and normalized.
        // The decoded-but-not-normalized path is needed because Spring MVC matches on the undecoded request URI and
        // leaves dot segments to a servlet container that does not exist in Lambda. "/admin/../public/info" still
        // reaches an "/admin/**" handler even though its canonical form is "/public/info", so matching on the
        // canonical path alone leaves that request reaching an admin handler with its filter skipped.
        //
        // The third spelling is the decoded path with path parameters removed from each segment. Spring removes
        // them before matching a handler, and the servlet specification has a container remove them before matching
        // url-patterns, but neither of the first two spellings does.
        //
        // Matching under any spelling over-selects, so a filter may run for a request that is ultimately routed
        // elsewhere. Under-selecting is the authorization bypass, so that is the direction chosen.
        String rawPath = AwsHttpServletRequest.contextRelativeRequestUri(request);
        String decodedPath = AwsHttpServletRequest.decodePath(rawPath);
        String canonicalPath = request.getPathInfo();
        if (canonicalPath == null || canonicalPath.isEmpty()) {
            canonicalPath = AwsHttpServletRequest.canonicalizePath(rawPath);
        }
        String parameterFreePath = AwsHttpServletRequest.stripPathParameters(decodedPath);
        // The parameter-free spelling is derived from the decoded one, so these two determine the result.
        String targetPath = canonicalPath + "|" + decodedPath;
        DispatcherType type = request.getDispatcherType();

        // only return the cached result if the filter list hasn't changed in the meanwhile
        if (getFilterHolders().size() == filtersSize && getFilterChainCache(type, targetPath, servlet) != null) {
            return getFilterChainCache(type, targetPath, servlet);
        }

        AwsServletRegistration servletRegistration = (AwsServletRegistration)servletContext.getServletRegistrations()
                .values().stream()
                .filter((Predicate<ServletRegistration>) servletRegistration1 -> ((AwsServletRegistration) servletRegistration1).getServlet().equals(servlet))
                .findFirst().orElse(null);

        FilterChainHolder chainHolder = new FilterChainHolder();

        Map<String, FilterHolder> registrations = getFilterHolders();
        if (registrations == null || registrations.size() == 0) {
            if (servletRegistration != null) {
                chainHolder.addFilter(new FilterHolder(new ServletExecutionFilter(servletRegistration), servletContext));
            }
            return chainHolder;
        }
        for (Map.Entry<String, FilterHolder> entry : registrations.entrySet()) {
            FilterHolder holder = entry.getValue();
            // we only check the dispatcher type if it's not empty. Otherwise we assume it's a REQUEST as per section 6.2.5
            // of servlet specs
            if (holder.getRegistration().getDispatcherTypes().size() > 0 && !holder.getRegistration().getDispatcherTypes().contains(type)) {
                continue;
            }
            for (String path : holder.getRegistration().getUrlPatternMappings()) {
                if (pathMatches(canonicalPath, path) || pathMatches(decodedPath, path)
                        || pathMatches(parameterFreePath, path)) {
                    chainHolder.addFilter(holder);
                    break;
                }
            }

            // TODO: We do not allow programmatic registration of servlets so we never check for servlet name
            // we assume we only ever have one servlet.
        }

        if (servletRegistration != null) {
            chainHolder.addFilter(new FilterHolder(new ServletExecutionFilter(servletRegistration), servletContext));
        }

        putFilterChainCache(type, targetPath, servlet, chainHolder);
        // update total filter size
        if (filtersSize != registrations.size()) {
            filtersSize = registrations.size();
        }
        return chainHolder;
    }


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    /**
     * Retrieves a filter chain from the cache. The cache is lazily loaded as filter chains are requested. If the chain
     * is not available in the cache, the method returns null. This method returns a new instance of FilterChainHolder
     * initialized with the cached list of {@link FilterHolder} objects
     * @param type The dispatcher type for the incoming request
     * @param targetPath The request path - this is extracted with the <code>getPath</code> method of the request object
     * @param servlet The final servlet in the filter chain (if any)
     * @return A populated FilterChainHolder
     */
    private FilterChainHolder getFilterChainCache(final DispatcherType type, final String targetPath, Servlet servlet) {
        TargetCacheKey key = new TargetCacheKey();
        key.setDispatcherType(type);
        key.setTargetPath(targetPath);
        if (servlet != null) {
            key.setServletName(servlet.getServletConfig().getServletName());
        }

        if (!filterCache.containsKey(key)) {
            return null;
        }

        return new FilterChainHolder(filterCache.get(key));
    }


    /**
     * Adds a filter chain to the local cache. The key for the filter chain in the cache is generated with the dispatcher
     * type and the path from the request. If we cannot compute the key for a filter chain, because either the path or
     * dispatcher are null, this method returns without saving the chain in the cache. It's up to the <code>getFilterChain</code>
     * method to retry this.
     * @param type DispatcherType from the incoming request
     * @param targetPath The target path in the API
     * @param servlet The final servlet in the filter chain (if any)
    * @param holder The FilterChainHolder object to save in the cache
     */
    private void putFilterChainCache(final DispatcherType type, final String targetPath, Servlet servlet, final FilterChainHolder holder) {
        TargetCacheKey key = new TargetCacheKey();
        key.setDispatcherType(type);
        key.setTargetPath(targetPath);
        if (servlet != null) {
            key.setServletName(servlet.getServletConfig().getServletName());
        }

        // we couldn't compute the hash code because either the target path or dispatcher type were null
        if (key.hashCode() == -1) {
            return;
        }
        filterCache.put(key, holder.getFilters());

    }


    /**
     * Checks if a mapping path matches the target path of the request. The mapping path can include wildcards. For example,
     * the filter configured for /echo/* will match for request coming to all sub-resources of /echo. If not path
     * @param target The target path from the incoming request
     * @param mapping The mapping path stored in the filter registration
     * @return true if the given mapping path can apply to the target, false otherwise.
     */
    boolean pathMatches(final String target, final String mapping) {
        // Matching is case-insensitive throughout. The exact-equality check below always lowercased both sides while
        // the segment comparison further down was case-sensitive, so the two halves of this method disagreed and a
        // differently-cased path could skip its filter. Over-selecting a filter is fail-safe; under-selecting one is
        // an authorization bypass, so both paths now compare case-insensitively.
        String finalTarget = target.toLowerCase(Locale.ENGLISH);
        String finalMapping = mapping.toLowerCase(Locale.ENGLISH);

        // easiest case, they are exactly the same
        if (finalTarget.equals(finalMapping)) {
            return true;
        }

        // strip first slash
        if (finalTarget.startsWith("/")) {
            finalTarget = finalTarget.replaceFirst("/", "");
        }
        if (finalMapping.startsWith("/")) {
            finalMapping = finalMapping.replaceFirst("/", "");
        }

        String[] targetParts = finalTarget.split(PATH_PART_SEPARATOR);
        String[] mappingParts = finalMapping.split(PATH_PART_SEPARATOR);

        // another simple case, the filter applies to everything
        if (mappingParts.length == 1 && mappingParts[0].equals("*")) {
            return true;
        }

        for (int i = 0; i < targetParts.length; i++) {
            if (mappingParts.length < i + 1) {
                // if we haven't matched anything yet we never will
                return false;
            }
            // the exact work doesn't match the and holder is not a wildcard
            if (!targetParts[i].equals(mappingParts[i])) {
                if (mappingParts[i].equals("*")) {
                    break;
                } else {
                    return false;
                }
            }
        }

        return true;
    }


    //-------------------------------------------------------------
    // Inner Class -
    //-------------------------------------------------------------

    /**
     * Object used as a key for the filter chain cache. It contains a target path and dispatcher type property. It overrides
     * the default <code>hashCode</code> and <code>equals</code> methods to return a consistent hash for comparison.
     */
    protected static class TargetCacheKey {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

        private String targetPath = normalizeTargetPath(null);
        private DispatcherType dispatcherType;
        private String servletName;


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

        /**
         * The hash code for a cache key is calculated from the normalized target path, the dispatcher type and the
         * servlet name, the same three fields <code>equals</code> compares.
         * @return An int representing the hash code fo the key
         */
        @Override
        public int hashCode() {
            return Objects.hash(targetPath, (dispatcherType == null ? null : dispatcherType.name()), servletName);
        }


        /**
         * Two keys are equal when their normalized target path, dispatcher type and servlet name are equal. This
         * compares the fields themselves rather than hash codes: distinct paths can share a hash code, and treating
         * that as a match would serve one path's cached filter chain, and so its authorization decision, for another.
         */
        @Override
        public boolean equals(Object key) {
            if (this == key) {
                return true;
            }
            if (key == null || !key.getClass().equals(this.getClass())) {
                return false;
            }
            TargetCacheKey other = (TargetCacheKey) key;
            return targetPath.equals(other.targetPath)
                    && dispatcherType == other.dispatcherType
                    && Objects.equals(servletName, other.servletName);
        }


        /**
         * Cleans a target path so that equivalent spellings share a cache entry:
         *  1. a null path is treated as "/"
         *  2. trim white spaces
         *  3. Remove "/" as last character if it is there
         *  4. Add "/" as first character if not there
         * This used to happen inside <code>hashCode</code> only, which made it part of equality by accident. It is
         * applied when the path is set so that equality depends on it explicitly.
         */
        private static String normalizeTargetPath(String path) {
            String normalized = (path == null ? "/" : path.trim());
            if (normalized.endsWith(PATH_PART_SEPARATOR)) {
                normalized = normalized.substring(0, normalized.length() - 1);
            }
            if (!normalized.startsWith(PATH_PART_SEPARATOR)) {
                normalized = PATH_PART_SEPARATOR + normalized;
            }
            return normalized;
        }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

        void setTargetPath(String targetPath) {
            this.targetPath = normalizeTargetPath(targetPath);
        }


        void setDispatcherType(DispatcherType dispatcherType) {
            this.dispatcherType = dispatcherType;
        }
        public void setServletName(String servletName) {
            this.servletName = servletName;
        }


    }

    @SuppressFBWarnings("URF_UNREAD_FIELD")
    private class ServletExecutionFilter implements Filter {

        private FilterConfig config;
        private AwsServletRegistration handlerServlet;
        private boolean initialized;

        public ServletExecutionFilter(AwsServletRegistration servletReg) {
            handlerServlet = servletReg;
            initialized = handlerServlet.getServlet().getServletInfo() != null;
        }

        @Override
        public void init(FilterConfig filterConfig)
                throws ServletException {
            if (initialized) {
                return;
            }
            config = filterConfig;
            handlerServlet.getServlet().init(handlerServlet.getServletConfig());
            initialized = true;
        }


        @Override
        public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse, FilterChain filterChain)
                throws IOException, ServletException {
            handlerServlet.getServlet().service(servletRequest, servletResponse);
            filterChain.doFilter(servletRequest, servletResponse);
        }


        @Override
        public void destroy() {

        }
    }
}
