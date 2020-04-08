/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import java.util.*;

/**
 * Stores information about a servlet registered with Serverless Java Container's <code>ServletContext</code>.
 */
public class AwsServletRegistration implements ServletRegistration, ServletRegistration.Dynamic, Comparable<AwsServletRegistration> {
    private String servletName;
    private Servlet servlet;
    private AwsServletContext ctx;
    private Map<String, String> initParameters;
    private int loadOnStartup;
    private String runAsRole;
    private boolean asyncSupported;
    private Map<String, AwsServletRegistration> servletPathMappings;


    public AwsServletRegistration(String name, Servlet s, AwsServletContext context) {
        servletName = name;
        servlet = s;
        ctx = context;
        initParameters = new HashMap<>();
        servletPathMappings = new HashMap<>();
        loadOnStartup = -1;
        asyncSupported = true;
    }

    @Override
    public Set<String> addMapping(String... strings) {
        Set<String> failedMappings = new HashSet<>();
        for (String s : strings) {
            if (servletPathMappings.containsKey(s)) {
                failedMappings.add(s);
                continue;
            }
            servletPathMappings.put(s, this);
        }
        return failedMappings;
    }

    @Override
    public Collection<String> getMappings() {
        return servletPathMappings.keySet();
    }

    @Override
    public String getRunAsRole() {
        return runAsRole;
    }

    @Override
    public String getName() {
        return servletName;
    }

    @Override
    public String getClassName() {
        return servlet.getClass().getName();
    }

    @Override
    public boolean setInitParameter(String s, String s1) {
        if (initParameters.containsKey(s)) {
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
        Set<String> failedParameters = new HashSet<>();
        for (Map.Entry<String, String> param : map.entrySet()) {
            if (initParameters.containsKey(param.getKey())) {
                failedParameters.add(param.getKey());
            }
            initParameters.put(param.getKey(), param.getValue());
        }
        return failedParameters;
    }

    @Override
    public Map<String, String> getInitParameters() {
        return initParameters;
    }

    public Servlet getServlet() {
        return servlet;
    }

    @Override
    public void setLoadOnStartup(int i) {
        loadOnStartup = i;
    }

    public int getLoadOnStartup() {
        return loadOnStartup;
    }

    @Override
    public Set<String> setServletSecurity(ServletSecurityElement servletSecurityElement) {
        return null;
    }

    @Override
    public void setMultipartConfig(MultipartConfigElement multipartConfigElement) {

    }

    @Override
    public void setRunAsRole(String s) {
        runAsRole = s;
    }

    @Override
    public void setAsyncSupported(boolean b) {
        asyncSupported = b;
    }

    @Override
    public int compareTo(AwsServletRegistration r) {
        return Integer.compare(loadOnStartup, r.getLoadOnStartup());
    }

    @Override
    @SuppressFBWarnings("HE_EQUALS_USE_HASHCODE")
    public boolean equals(Object r) {
        if (r == null || !AwsServletRegistration.class.isAssignableFrom(r.getClass())) {
            return false;
        }
        return ((AwsServletRegistration)r).getName().equals(getName()) && ((AwsServletRegistration)r).getServlet() == getServlet();
    }

    public boolean isAsyncSupported() {
        return asyncSupported;
    }

    public ServletConfig getServletConfig() {
        return new ServletConfig() {
            @Override
            public String getServletName() {
                return servletName;
            }

            @Override
            public ServletContext getServletContext() {
                return ctx;
            }

            @Override
            public String getInitParameter(String s) {
                return initParameters.get(s);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(initParameters.keySet());
            }
        };
    }
}
