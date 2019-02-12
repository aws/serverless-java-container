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

import java.util.Map;

/**
 * This implementation of the <code>FilterChainManager</code> object uses the <code>AwsServletContext</code> object
 * to extract a list of <code>FilterHolder</code>s
 */
public class AwsFilterChainManager extends FilterChainManager<AwsServletContext> {

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    /**
     * Creates a new FilterChainManager for the given servlet context
     * @param context An initialized AwsServletContext object
     */
    AwsFilterChainManager(AwsServletContext context) {
        super(context);
    }

    //-------------------------------------------------------------
    // Implementation - FilterChainManager
    //-------------------------------------------------------------

    /**
     * Returns the filter holders stored in the <code>AwsServletContext</code> object
     * @return The map of filter holders
     */
    @Override
    protected Map<String, FilterHolder> getFilterHolders() {
        return servletContext.getFilterHolders();
    }
}
