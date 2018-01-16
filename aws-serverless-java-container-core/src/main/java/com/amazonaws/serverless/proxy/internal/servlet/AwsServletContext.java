/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.Filter;
import javax.servlet.FilterRegistration;
import javax.servlet.RequestDispatcher;
import javax.servlet.Servlet;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.SessionCookieConfig;
import javax.servlet.SessionTrackingMode;
import javax.servlet.descriptor.JspConfigDescriptor;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;


/**
 * Basic implementation of the <code>ServletContext</code> object. Because this library is not a complete container
 * implementation the majority of methods in this object return a NotImplementedException or null. Supported properties
 * are <code>initParameters</code>, <code>attributes</code>, and <code>filters</code>.
 */
public class AwsServletContext
        implements ServletContext {

    //-------------------------------------------------------------
    // Constants - Public
    // -------------------------------------------------------------
    public static final int SERVLET_API_MAJOR_VERSION = 3;
    public static final int SERVLET_API_MINOR_VERSION = 1;
    public static final String SERVER_INFO = LambdaContainerHandler.SERVER_INFO + "/" + SERVLET_API_MAJOR_VERSION + "." + SERVLET_API_MINOR_VERSION;


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------
    private Map<String, FilterHolder> filters;
    private Map<String, Object> attributes;
    private Map<String, String> initParameters;
    private AwsLambdaServletContainerHandler containerHandler;
    private Logger log = LoggerFactory.getLogger(AwsServletContext.class);


    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------
    private static AwsServletContext instance;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsServletContext(AwsLambdaServletContainerHandler containerHandler) {
        this.containerHandler = containerHandler;
        this.attributes = new HashMap<>();
        this.initParameters = new HashMap<>();
        this.filters = new LinkedHashMap<>();
    }


    //-------------------------------------------------------------
    // Implementation - ServletContext
    //-------------------------------------------------------------

    public static void clearServletContextCache() {
        instance = null;
    }


    @Override
    public String getContextPath() {
        // servlets are always at the root.
        return "";
    }


    @Override
    public ServletContext getContext(String s) {
        // all urls point to the same context.
        return this;
    }


    @Override
    public int getMajorVersion() {
        return SERVLET_API_MAJOR_VERSION;
    }


    @Override
    public int getMinorVersion() {
        return SERVLET_API_MINOR_VERSION;
    }


    @Override
    public int getEffectiveMajorVersion() {
        return SERVLET_API_MAJOR_VERSION;
    }


    @Override
    public int getEffectiveMinorVersion() {
        return SERVLET_API_MINOR_VERSION;
    }


    @Override
    @SuppressFBWarnings("PATH_TRAVERSAL_IN") // suppressing because we are using the getValidFilePath
    public String getMimeType(String s) {
        try {
            String validatedPath = SecurityUtils.getValidFilePath(s);
            if (s.startsWith("file:")) { // Support paths such as file:/D:/something/hello.txt
                return Files.probeContentType(Paths.get(URI.create(validatedPath)));
            } else if (s.startsWith("/")) { // Support paths such as file:/D:/something/hello.txt
                    return Files.probeContentType(Paths.get(URI.create("file://" + validatedPath)));
            } else {
                return Files.probeContentType(Paths.get(validatedPath));
            }
        } catch (IOException e) {
            log.warn("Could not find content type for file:  " + SecurityUtils.encode(s), e);
            return null;
        }
    }


    @Override
    public Set<String> getResourcePaths(String s) {
        // We do not know the resources from here, we'd need to get the list from the frameworks.
        // TODO: Perhaps declare a new reader interface that can be implemented in framework-specific modules
        throw new UnsupportedOperationException();
    }


    @Override
    public URL getResource(String s) throws MalformedURLException {
        return AwsServletContext.class.getResource(s);
    }


    @Override
    public InputStream getResourceAsStream(String s) {
        return AwsServletContext.class.getResourceAsStream(s);
    }


    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return new AwsProxyRequestDispatcher(s, containerHandler);
    }


    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Servlet getServlet(String s) throws ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Enumeration<Servlet> getServlets() {
        throw new UnsupportedOperationException();
    }


    @Override
    @Deprecated
    public Enumeration<String> getServletNames() {
        throw new UnsupportedOperationException();
    }


    @Override
    public void log(String s) {
        log.info(SecurityUtils.encode(s));
    }


    @Override
    @Deprecated
    public void log(Exception e, String s) {
        log.error(SecurityUtils.encode(s), e);
    }


    @Override
    public void log(String s, Throwable throwable) {
        log.error(SecurityUtils.encode(s), throwable);
    }


    @Override
    public String getRealPath(String s) {
        String absPath = null;
        URL fileUrl = ClassLoader.getSystemResource(s);
        if (fileUrl != null) {
            try {
                absPath = new File(fileUrl.toURI()).getAbsolutePath();
            } catch (URISyntaxException e) {
                log.error("Error while looking for real path: {}", SecurityUtils.encode(s), SecurityUtils.encode(e.getMessage()));
            }
        }
        return absPath;
    }


    @Override
    public String getServerInfo() {
        return SERVER_INFO;
    }


    @Override
    public String getInitParameter(String s) {
        return initParameters.get(s);
    }


    @Override
    public Enumeration<String> getInitParameterNames() {
        return Collections.enumeration(initParameters.keySet());
    }


    @Override
    public boolean setInitParameter(String s, String s1) {
        initParameters.put(s, s1);
        return true;
    }


    @Override
    public Object getAttribute(String s) {
        return attributes.get(s);
    }


    @Override
    public Enumeration<String> getAttributeNames() {
        return Collections.enumeration(attributes.keySet());
    }


    @Override
    public void setAttribute(String s, Object o) {
        attributes.put(s, o);
    }


    @Override
    public void removeAttribute(String s) {
        attributes.remove(s);
    }


    @Override
    public String getServletContextName() {
        // TODO: This can also come from a reader interface
        throw new UnsupportedOperationException();
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, String s1) {
        log("Called addServlet: " + s1);
        log("Implemented frameworks are responsible for registering servlets");
        throw new UnsupportedOperationException();
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
        log("Called addServlet: " + servlet.getClass().getName());
        log("Implemented frameworks are responsible for registering servlets");
        throw new UnsupportedOperationException();
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
        log("Called addServlet: " + aClass.getName());
        log("Implemented frameworks are responsible for registering servlets");
        throw new UnsupportedOperationException();
    }


    @Override
    public <T extends Servlet> T createServlet(Class<T> aClass) throws ServletException {
        log("Called createServlet: " + aClass.getName());
        log("Implemented frameworks are responsible for creating servlets");
        throw new UnsupportedOperationException();
    }


    @Override
    public ServletRegistration getServletRegistration(String s) {
        // TODO: This could come from the reader interface
        return null;
    }


    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        // TODO: This could come from the reader interface
        return null;
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String name, String filterClass) {
        try {
            Class<?> newFilterClass = getClassLoader().loadClass(filterClass);
            if (!newFilterClass.isAssignableFrom(Filter.class)) {
                throw new IllegalArgumentException(filterClass + " does not implement Filter");
            }
            @SuppressWarnings("unchecked")
            Class<? extends Filter> filterCastClass = (Class<? extends Filter>)newFilterClass;

            return addFilter(name, filterCastClass);
        } catch (ClassNotFoundException e) {
            log.error("Could not find filter class", e);
            throw new IllegalStateException("Filter class " + filterClass + " not found");
        }
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String name, Filter filter) {
        if (name == null || "".equals(name.trim()))
            throw new IllegalArgumentException("Missing filter name");

        // filter already exists, we do nothing
        if (filters.containsKey(name)) {
            return null;
        } else {
            log.debug("Adding filter '{}' from {}", SecurityUtils.encode(name), SecurityUtils.encode(filter.toString()));
        }

        FilterHolder newFilter = new FilterHolder(name, filter, this);

        filters.put(newFilter.getFilterName(), newFilter);
        return newFilter.getRegistration();
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String name, Class<? extends Filter> filterClass) {
        try {
            log.debug("Adding filter '{}' from {}", SecurityUtils.encode(name), SecurityUtils.encode(filterClass.getName()));
            Filter newFilter = createFilter(filterClass);
            return addFilter(name, newFilter);
        } catch (ServletException e) {
            // TODO: There is no clear indication in the servlet specs on whether we should throw an exception here.
            // See JavaDoc here: http://docs.oracle.com/javaee/7/api/javax/servlet/ServletContext.html#addFilter-java.lang.String-java.lang.Class-
            log.error("Could not register filter: ", e);
        }
        return null;
    }


    @Override
    public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
        try {
            return aClass.newInstance();
        } catch (InstantiationException | IllegalAccessException e) {
            log.error("Could not initialize filter class " + aClass.getName(), e);
            throw new ServletException();
        }
    }


    @Override
    public FilterRegistration getFilterRegistration(String s) {

        if (!filters.containsKey(s)) {
            return null;
        }
        return filters.get(s).getRegistration();
    }


    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        Map<String, FilterRegistration> registrations = new LinkedHashMap<>();
        for (Map.Entry<String, FilterHolder> entry : filters.entrySet()) {
            registrations.put(entry.getKey(), entry.getValue().getRegistration());
        }

        return registrations;
    }


    Map<String, FilterHolder> getFilterHolders() {
        return filters;
    }


    @Override
    public SessionCookieConfig getSessionCookieConfig() {
        return null;
    }


    @Override
    public void setSessionTrackingModes(Set<SessionTrackingMode> set) {

    }


    @Override
    public Set<SessionTrackingMode> getDefaultSessionTrackingModes() {
        return null;
    }


    @Override
    public Set<SessionTrackingMode> getEffectiveSessionTrackingModes() {
        return null;
    }


    @Override
    public void addListener(String s) {

    }


    @Override
    public <T extends EventListener> void addListener(T t) {

    }


    @Override
    public void addListener(Class<? extends EventListener> aClass) {

    }


    @Override
    public <T extends EventListener> T createListener(Class<T> aClass) throws ServletException {
        return null;
    }


    @Override
    public JspConfigDescriptor getJspConfigDescriptor() {
        return null;
    }


    @Override
    public ClassLoader getClassLoader() {
        // for the time being we return the default class loader. We may want to let developers override this int the
        // future.
        return ClassLoader.getSystemClassLoader();
    }


    @Override
    public void declareRoles(String... strings) {

    }


    @Override
    public String getVirtualServerName() {
        return null;
    }
}