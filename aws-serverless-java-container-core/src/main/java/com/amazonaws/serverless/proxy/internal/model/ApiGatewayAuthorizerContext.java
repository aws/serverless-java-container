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
package com.amazonaws.serverless.proxy.internal.model;

import java.util.HashMap;

/**
 * Custom authorizer context object for the API Gateway request context.
 */
public class ApiGatewayAuthorizerContext extends HashMap<String, String> {

    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getPrincipalId() {
        return get("principalId");
    }


    public void setPrincipalId(String principalId) {
        put("principalId", principalId);
    }

    public String getContextValue(String key) {
        return get(key);
    }
}
