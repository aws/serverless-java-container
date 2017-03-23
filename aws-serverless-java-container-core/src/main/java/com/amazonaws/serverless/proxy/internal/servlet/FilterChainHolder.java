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
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * Implementation of the <code>FilterChain</code> interface. FilterChainHolder objects should be accessed through the
 * <code>FilterChainManager</code>. Once a filter chain is loaded, use the <code>doFilter</code> emthod to run the chain
 * during a request lifecycle
 */
public class FilterChainHolder implements FilterChain {
    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private List<FilterHolder> filters;
    private int currentFilter;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Creates a new instance of a filter chain holder
     * @param allFilters A populated list of <code>FilterHolder</code> objects
     */
    public FilterChainHolder(List<FilterHolder> allFilters) {
        filters = allFilters;
        currentFilter = -1;
    }

    /**
     * Creates a new empty <code>FilterChainHolder</code>
     */
    public FilterChainHolder() {
        this(new ArrayList<>());
    }

    //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    /**
     * Add a filter to the chain.
     * @param newFilter The filter to be added at the end of the chain
     */
    public void addFilter(FilterHolder newFilter) {
        filters.add(newFilter);
    }

    //-------------------------------------------------------------
    // Implementation - FilterChain
    //-------------------------------------------------------------

    @Override
    public void doFilter(ServletRequest servletRequest, ServletResponse servletResponse) throws IOException, ServletException {
        currentFilter++;
        if (filters == null || filters.size() == 0 || currentFilter > filters.size() - 1) {
            return;
        }
        // TODO: We do check for async filters here
        FilterHolder holder = filters.get(currentFilter);

        if (!holder.isFilterInitialized()) {
            holder.init();
        }
        holder.getFilter().doFilter(servletRequest, servletResponse, this);
    }
}