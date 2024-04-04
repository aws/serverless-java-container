/*
 * Copyright 2024 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy;

/**
 * Utility class that helps determine the initialization type
 */
public final class InitializationTypeHelper {

    private static final String INITIALIZATION_TYPE_ENVIRONMENT_VARIABLE_NAME = "AWS_LAMBDA_INITIALIZATION_TYPE";
    private static final String INITIALIZATION_TYPE_ON_DEMAND = "on-demand";
    private static final String INITIALIZATION_TYPE = System.getenv().getOrDefault(INITIALIZATION_TYPE_ENVIRONMENT_VARIABLE_NAME,
            INITIALIZATION_TYPE_ON_DEMAND);
    private static final boolean ASYNC_INIT_DISABLED = !INITIALIZATION_TYPE.equals(INITIALIZATION_TYPE_ON_DEMAND);

    public static boolean isAsyncInitializationDisabled() {
        return ASYNC_INIT_DISABLED;
    }

    public static String getInitializationType() {
        return INITIALIZATION_TYPE;
    }
}
