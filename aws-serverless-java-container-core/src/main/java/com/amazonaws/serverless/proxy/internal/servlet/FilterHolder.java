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

import javax.servlet.*;
import javax.servlet.annotation.WebFilter;
import javax.servlet.annotation.WebInitParam;
import java.util.*;

/**
 * Wrapper for a servlet filter object. This object includes the filter itself, the filter name, the initialization
 * parameters, as well as the registration object with its mappings. Filter holders are stored in the <code>AwsServletContext</code>
 * object and are used by the by the <code>FilterChainHandler</code> object to process a request.
 */
public class FilterHolder {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private Filter filter;
    private FilterConfig filterConfig = new Config();
    private Registration registration;
    private String filterName;
    private Map<String, String> initParameters;

    private ServletContext servletContext;
    private boolean filterInitialized;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public FilterHolder(Filter newFilter, ServletContext context) {
        this(newFilter.getClass().getName(), newFilter, context);
    }


    /**
     * Creates a new filter holder for the given Filter object. The object is initialized with an empty map of parameters
     * and a registration with no mappings
     * @param name The filter name
     * @param newFilter The filter object to be registered against the name
     * @param context The ServletContext this object was initialized by
     */
    public FilterHolder(String name, Filter newFilter, ServletContext context) {
        filter = newFilter;
        servletContext = context;
        filterInitialized = false;

        if (isAnnotated()) {
            filterName = readAnnotatedFilterName();
            initParameters = readAnnotatedInitParams();
            registration = new Registration(getAnnotation());
        } else {
            filterName = name;
            initParameters = new HashMap<>();
            registration = new Registration();
        }
    }


    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Initializes the wrapped filter and sets the <code>isFilterInitialized</code> property to true. This should be called
     * before invoking a filter if the result of the <code>isFilterInitialized()</code> method is false.
     * @throws ServletException Propagates any servlet exception thrown by the filter initialization
     */
    public void init() throws ServletException {
        if (this.filterInitialized) {
            return;
        }
        this.getFilter().init(filterConfig);
        this.filterInitialized = true;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    /**
     * Checks whether the filter this holder is responsible for has been initialized. This method should be checked before
     * calling a filter, if it returns false then you should call the <code>init</code> method.
     * @return True if the filter has been initialized before, false otherwise
     */
    public boolean isFilterInitialized() {
        return filterInitialized;
    }


    /**
     * Returns the filter object
     * @return the filter object
     */
    public Filter getFilter() {
        return filter;
    }


    /**
     * The filter config object implementation. This is the default <code>Config</code> object implemented in this file
     * @return The filter config object
     */
    public FilterConfig getFilterConfig() {
        return filterConfig;
    }


    /**
     * Returns the Registration object for the filter. The <code>Registration</code> object defined in this file implements
     * both <code>FilterRegistration</code> and <code>FilterRegistration.Dynamic</code>
     * @return The registration object
     */
    public Registration getRegistration() {
        return registration;
    }


    /**
     * The name associated with the filter
     * @return The name for the filter
     */
    public String getFilterName() {
        return filterName;
    }


    public void setFilterName(String filterName) {
        this.filterName = filterName;
    }


    /**
     * The map of initialization parameters passed to the filter
     * @return The Map of initialization parameters used for the filter
     */
    public Map<String, String> getInitParameters() {
        return initParameters;
    }


    /**
     * The servlet context that initialized the filter
     * @return The servlet context the filter was initialized on
     */
    public ServletContext getServletContext() {
        return servletContext;
    }


    public boolean isAnnotated() {
        return filter.getClass().isAnnotationPresent(WebFilter.class);
    }


    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private String readAnnotatedFilterName() {
        if (isAnnotated()) {
            WebFilter regAnnotation = filter.getClass().getAnnotation(WebFilter.class);
            if (!"".equals(regAnnotation.filterName().trim())) {
                return regAnnotation.filterName();
            } else {
                return filter.getClass().getName();
            }
        } else {
            return null;
        }
    }


    private Map<String, String> readAnnotatedInitParams() {
        Map<String, String> initParams = new HashMap<>();
        if (isAnnotated()) {
            WebFilter regAnnotation = filter.getClass().getAnnotation(WebFilter.class);
            for (WebInitParam param : regAnnotation.initParams()) {
                initParams.put(param.name(), param.value());
            }
        }

        return initParams;
    }


    private WebFilter getAnnotation() {
        if (isAnnotated()) {
            return filter.getClass().getAnnotation(WebFilter.class);
        } else {
            return null;
        }
    }


    //-------------------------------------------------------------
    // Inner Class -
    //-------------------------------------------------------------

    /**
     * Registration class for the filter. This object stores the servlet names and the url patterns the filter is
     * associated with.
     */
    @SuppressFBWarnings("URF_UNREAD_FIELD")
    protected class Registration implements FilterRegistration.Dynamic {

        //-------------------------------------------------------------
        // Variables - Private
        //-------------------------------------------------------------

        private List<String> urlPatterns;
        private List<DispatcherType> dispatcherTypes;
        private boolean asyncSupported;


        //-------------------------------------------------------------
        // Constructors
        //-------------------------------------------------------------

        public Registration() {
            urlPatterns = new ArrayList<>();
            dispatcherTypes = new ArrayList<>();
            asyncSupported = false;
        }


        public Registration(WebFilter annotation) {
            urlPatterns = new ArrayList<>();
            dispatcherTypes = new ArrayList<>();

            EnumSet<DispatcherType> dispatchers = EnumSet.noneOf(DispatcherType.class);
            dispatchers.addAll(Arrays.asList(annotation.dispatcherTypes()));

            if (annotation.value().length > 0) {
                addMappingForUrlPatterns(dispatchers, true, annotation.value());
            }

            if (annotation.urlPatterns().length > 0) {
                addMappingForUrlPatterns(dispatchers, true, annotation.urlPatterns());
            }

            asyncSupported = annotation.asyncSupported();
        }


        //-------------------------------------------------------------
        // Implementation - Dynamic
        //-------------------------------------------------------------


        @Override
        public void setAsyncSupported(boolean b) {
            asyncSupported = b;
        }


        //-------------------------------------------------------------
        // Implementation - FilterRegistration
        //-------------------------------------------------------------


        @Override
        public void addMappingForServletNames(EnumSet<DispatcherType> types, boolean isLast, String... servlets) {
            throw new UnsupportedOperationException();
        }


        @Override
        public Collection<String> getServletNameMappings() {
            return null;
        }


        @Override
        public void addMappingForUrlPatterns(EnumSet<DispatcherType> types, boolean isLast, String... patterns) {
            if (types == null) {
                dispatcherTypes.add(DispatcherType.REQUEST);
                dispatcherTypes.add(DispatcherType.ASYNC);
            } else {
                dispatcherTypes.addAll(types);
            }

            for (String mapping : patterns) {
                if (!validateMappingPath(mapping)) {
                    throw new IllegalArgumentException(
                            "Invalid path mapping, wildcards should be the last part of a path: " + mapping);
                }
            }

            if (isLast) {
                urlPatterns.addAll(Arrays.asList(patterns));
            } else {
                List<String> newUrlList = new ArrayList<>();
                newUrlList.addAll(Arrays.asList(patterns));
                newUrlList.addAll(urlPatterns);
                urlPatterns = newUrlList;
            }
        }


        @Override
        public Collection<String> getUrlPatternMappings() {
            return urlPatterns;
        }


        //-------------------------------------------------------------
        // Implementation - Registration
        //-------------------------------------------------------------


        @Override
        public String getName() {
            return filterName;
        }


        @Override
        public String getClassName() {
            return filter.getClass().getName();
        }


        @Override
        public boolean setInitParameter(String s, String s1) {
            if (initParameters.get(s) != null) {
                return false;
            }
            initParameters.put(s, s1);
            return true;
        }


        @Override
        public String getInitParameter(String s) {
            return initParameters.get(s);
        }


        @Override
        public Set<String> setInitParameters(Map<String, String> map) {
            Set<String> conflicts = new LinkedHashSet<>();
            for (Map.Entry<String, String> entry : map.entrySet()) {
                if (initParameters.get(entry.getKey()) != null) {
                    conflicts.add(entry.getKey());
                } else {
                    initParameters.put(entry.getKey(), entry.getValue());
                }
            }
            return conflicts;
        }


        @Override
        public Map<String, String> getInitParameters() {
            return initParameters;
        }


        //-------------------------------------------------------------
        // Methods - Getter/Setter
        //-------------------------------------------------------------

        public List<DispatcherType> getDispatcherTypes() {
            return dispatcherTypes;
        }


        //-------------------------------------------------------------
        // Methods - Private
        //-------------------------------------------------------------

        /**
         * Validates a path mapping sent to the registration object. This method currently only checks for path parts
         * after a wildcard.
         * @param mapping The mapping path to be validated
         * @return True if this is a valid mapping, false otherwise.
         */
        private boolean validateMappingPath(String mapping) {
            String[] parts = mapping.split(FilterChainManager.PATH_PART_SEPARATOR);

            int wildcardPosition = -1;
            for (int i = 0; i < parts.length; i++) {
                if (wildcardPosition > -1 && i > wildcardPosition) {
                    return false;
                }
                if (parts[i].trim().equals("*")) {
                    wildcardPosition = i;
                }
            }
            return true;
        }
    }

    /**
     * Default implementation of the <code>FilterConfig</code> object.
     */
    class Config implements FilterConfig {

        //-------------------------------------------------------------
        // Implementation - FilterConfig
        //-------------------------------------------------------------

        @Override
        public String getFilterName() {
            return filterName;
        }


        @Override
        public ServletContext getServletContext() {
            return servletContext;
        }


        @Override
        public String getInitParameter(String s) {
            return initParameters.get(s);
        }


        @Override
        public Enumeration<String> getInitParameterNames() {
            return Collections.enumeration(initParameters.keySet());
        }
    }
}
