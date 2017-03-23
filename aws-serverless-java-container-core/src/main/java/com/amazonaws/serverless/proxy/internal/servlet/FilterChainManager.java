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

import javax.servlet.*;
import javax.servlet.http.HttpServletRequest;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * This object in in charge of matching a servlet request to a set of filter, creating the filter chain for a request,
 * and cache filter chains that were already loaded for re-use. This object should be used by the framework-specific
 * implementations that use the <code>HttpServletRequest</code> and <code>HttpServletResponse</code> objects.
 *
 * For example, the Spring implementation creates the ServletContext when the application is initialized the first time
 * and creates a FitlerChainManager to execute its filters for each request.
 */
public abstract class FilterChainManager<ServletContextType> {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    // we use the synchronizedMap because we do not expect high concurrency on this object. Lambda only allows one
    // event at a time per container
    private Map<TargetCacheKey, FilterChainHolder> filterCache = Collections.synchronizedMap(new HashMap<TargetCacheKey, FilterChainHolder>());

    //-------------------------------------------------------------
    // Variables - Protected
    //-------------------------------------------------------------

    protected static final String PATH_PART_SEPARATOR = "/";
    protected ServletContextType servletContext;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    protected FilterChainManager(ServletContextType context) {
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
     * @return A <code>FilterChainHolder</code> object that can be used to apply the filters to the request
     */
    public FilterChainHolder getFilterChain(HttpServletRequest request) {
        String targetPath = request.getServletPath();
        DispatcherType type = request.getDispatcherType();

        if (getFilterChainCache(type, targetPath) != null) {
            return getFilterChainCache(type, targetPath);
        }

        FilterChainHolder chainHolder = new FilterChainHolder();

        Map<String, FilterHolder> registrations = getFilterHolders();
        if (registrations == null || registrations.size() == 0) {
            return chainHolder;
        }
        for (String name : registrations.keySet()) {
            FilterHolder holder = registrations.get(name);
            // we only check the dispatcher type if it's not empty. Otherwise we assume it's a REQUEST as per section 6.2.5
            // of servlet specs
            if (holder.getRegistration().getDispatcherTypes().size() > 0 && !holder.getRegistration().getDispatcherTypes().contains(type)) {
                continue;
            }
            for (String path : holder.getRegistration().getUrlPatternMappings()) {
                if (pathMatches(targetPath, path)) {
                    chainHolder.addFilter(holder);
                }
            }

            // TODO: We do not allow programmatic registration of servlets so we never check for servlet name
            // we assume we only ever have one servlet.
        }

        putFilterChainCache(type, targetPath, chainHolder);
        return chainHolder;
    }

    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    /**
     * Retrieves a filter chain from the cache. The cache is lazily loaded as filter chains are requested. If the chain
     * is not available in the cache, the method returns null.
     * @param type The dispatcher type for the incoming request
     * @param targetPath The request path - this is extracted with the <code>getPath</code> method of the request object
     * @return A populated FilterChainHolder
     */
    protected FilterChainHolder getFilterChainCache(DispatcherType type, String targetPath) {
        TargetCacheKey key = new TargetCacheKey();
        key.setDispatcherType(type);
        key.setTargetPath(targetPath);

        return filterCache.get(key);
    }

    /**
     * Adds a filter chain to the local cache. The key for the filter chain in the cache is generated with the dispatcher
     * type and the path from the request. If we cannot compute the key for a filter chain, because either the path or
     * dispatcher are null, this method returns without saving the chain in the cache. It's up to the <code>getFilterChain</code>
     * method to retry this.
     * @param type DispatcherType from the incoming request
     * @param targetPath The target path in the API
     * @param holder The FilterChainHolder object to save in the cache
     */
    protected void putFilterChainCache(DispatcherType type, String targetPath, FilterChainHolder holder) {
        TargetCacheKey key = new TargetCacheKey();
        key.setDispatcherType(type);
        key.setTargetPath(targetPath);

        // we couldn't compute the hash code because either the target path or dispatcher type were null
        if (key.hashCode() == -1) {
            return;
        }

        filterCache.put(key, holder);
    }

    /**
     * Checks if a mapping path matches the target path of the request. The mapping path can include wildcards. For example,
     * the filter configured for /echo/* will match for request coming to all sub-resources of /echo. If not path
     * @param target The target path from the incoming request
     * @param mapping The mapping path stored in the filter registration
     * @return true if the given mapping path can apply to the target, false otherwise.
     */
    protected boolean pathMatches(String target, String mapping) {
        // easiest case, they are exactly the same
        if (target.toLowerCase().equals(mapping.toLowerCase())) {
            return true;
        }

        String finalTarget = target;
        String finalMapping = mapping;
        // strip first slash
        if (target.startsWith("/")) {
            finalTarget = target.replaceFirst("/", "");
        }
        if (mapping.startsWith("/")) {
            finalMapping = mapping.replaceFirst("/", "");
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
            if (!targetParts[i].equals(mappingParts[i]) && !mappingParts[i].equals("*")) {
                return false;
            } else {
                // stop the loop, we have found a wildcard and all path parts prior to this matched.
                break;
            }

        }

        return true;
    }

    /**
     * Object used as a key for the filter chain cache. It contains a target path and dispatcher type property. It overrides
     * the default <code>hashCode</code> and <code>equals</code> methods to return a consistent hash for comparison.
     */
    private class TargetCacheKey {
        private String targetPath;
        private DispatcherType dispatcherType;

        public String getTargetPath() {
            return targetPath;
        }

        public void setTargetPath(String targetPath) {
            this.targetPath = targetPath;
        }

        public DispatcherType getDispatcherType() {
            return dispatcherType;
        }

        public void setDispatcherType(DispatcherType dispatcherType) {
            this.dispatcherType = dispatcherType;
        }

        @Override
        public int hashCode() {
            if (targetPath == null || dispatcherType == null) {
                return -1;
            }
            return (targetPath + ":" + dispatcherType.name()).hashCode();
        }

        @Override
        public boolean equals(Object key) {
            if (!key.getClass().isAssignableFrom(TargetCacheKey.class)) {
                return false;
            } else {
                return hashCode() == key.hashCode();
            }
        }
    }
}
