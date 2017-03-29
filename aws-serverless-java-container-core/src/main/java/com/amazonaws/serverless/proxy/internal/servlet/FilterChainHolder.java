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
     * Creates a new empty <code>FilterChainHolder</code>
     */
    public FilterChainHolder() {
        this(new ArrayList<>());
    }


    /**
     * Creates a new instance of a filter chain holder
     * @param allFilters A populated list of <code>FilterHolder</code> objects
     */
    public FilterChainHolder(List<FilterHolder> allFilters) {
        filters = allFilters;
        currentFilter = -1;
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
        // TODO: We do not check for async filters here

        FilterHolder holder = filters.get(currentFilter);

        if (!holder.isFilterInitialized()) {
            holder.init();
        }
        holder.getFilter().doFilter(servletRequest, servletResponse, this);
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


    /**
     * Returns the number of filters loaded in the chain holder
     * @return The number of filters in the chain holder. If the filter chain is null then this will return 0
     */
    public int filterCount() {
        if (filters == null) {
            return 0;
        } else {
            return filters.size();
        }
    }


    /**
     * Get the <code>FilterHolder</code> object from the chain at the given index.
     * @param idx The index in the chain. Use the <code>filterCount</code> method to get the filter count
     * @return A populated FilterHolder object
     */
    public FilterHolder getFilter(int idx) {
        if (filters == null) {
            return null;
        } else {
            return filters.get(idx);
        }
    }
}
