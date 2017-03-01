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


import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;

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

import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.Enumeration;
import java.util.EventListener;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;


public class AwsProxyServletContext
        implements ServletContext {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    private static final int SERVLET_API_MAJOR_VERSION = 3;
    private static final int SERVLET_API_MINOR_VERSION = 1;


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private AwsProxyRequest awsProxyRequest;
    private Context lambdaContext;
    private Map<String, Object> attributes;
    private Map<String, String> initParameters;


    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static AwsProxyServletContext instance;


    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    private AwsProxyServletContext(AwsProxyRequest awsProxyRequest, Context lambdaContext) {
        this.awsProxyRequest = awsProxyRequest;
        this.lambdaContext = lambdaContext;

        this.attributes = new HashMap<>();
        this.initParameters = new HashMap<>();
    }


    //-------------------------------------------------------------
    // Implementation - ServletContext
    //-------------------------------------------------------------


    @Override
    public String getContextPath() {
        return "/" + awsProxyRequest.getRequestContext().getStage();
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
    public String getMimeType(String s) {
        try {
            return Files.probeContentType(Paths.get(s));
        } catch (IOException e) {
            e.printStackTrace();
            return null;
        }
    }


    @Override
    public Set<String> getResourcePaths(String s) {
        // TODO: Define a reader interface that we can implement for each container
        return null;
    }


    @Override
    public URL getResource(String s) throws MalformedURLException {
        return getClass().getResource(s);
    }


    @Override
    public InputStream getResourceAsStream(String s) {
        return getClass().getResourceAsStream(s);
    }


    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return null;
    }


    @Override
    public RequestDispatcher getNamedDispatcher(String s) {
        return null;
    }


    @Override
    public Servlet getServlet(String s) throws ServletException {
        return null;
    }


    @Override
    public Enumeration<Servlet> getServlets() {
        return null;
    }


    @Override
    public Enumeration<String> getServletNames() {
        return null;
    }


    @Override
    public void log(String s) {
        lambdaContext.getLogger().log(s);
    }


    @Override
    public void log(Exception e, String s) {
        lambdaContext.getLogger().log(s);
        lambdaContext.getLogger().log(e.getMessage());
    }


    @Override
    public void log(String s, Throwable throwable) {
        lambdaContext.getLogger().log(s);
        lambdaContext.getLogger().log(throwable.getMessage());
    }


    @Override
    public String getRealPath(String s) {
        return null;
    }


    @Override
    public String getServerInfo() {
        return null;
    }


    @Override
    public String getInitParameter(String s) {
        return null;
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
        return null;
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, String s1) {
        // TODO: Should we throw an unimplemented exception here?
        return null;
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, Servlet servlet) {
        return null;
    }


    @Override
    public ServletRegistration.Dynamic addServlet(String s, Class<? extends Servlet> aClass) {
        return null;
    }


    @Override
    public <T extends Servlet> T createServlet(Class<T> aClass) throws ServletException {
        return null;
    }


    @Override
    public ServletRegistration getServletRegistration(String s) {
        return null;
    }


    @Override
    public Map<String, ? extends ServletRegistration> getServletRegistrations() {
        return null;
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String s, String s1) {
        return null;
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String s, Filter filter) {
        return null;
    }


    @Override
    public FilterRegistration.Dynamic addFilter(String s, Class<? extends Filter> aClass) {
        return null;
    }


    @Override
    public <T extends Filter> T createFilter(Class<T> aClass) throws ServletException {
        return null;
    }


    @Override
    public FilterRegistration getFilterRegistration(String s) {
        return null;
    }


    @Override
    public Map<String, ? extends FilterRegistration> getFilterRegistrations() {
        return null;
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
        return null;
    }


    @Override
    public void declareRoles(String... strings) {

    }


    @Override
    public String getVirtualServerName() {
        return null;
    }


    public static ServletContext getInstance(AwsProxyRequest request, Context lambdaContext) {
        if (instance == null) {
            instance = new AwsProxyServletContext(request, lambdaContext);
        }

        return instance;
    }
}
