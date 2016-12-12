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
package com.amazonaws.serverless.proxy.internal.testutils;

import com.amazonaws.services.lambda.runtime.ClientContext;
import com.amazonaws.services.lambda.runtime.CognitoIdentity;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.LambdaLogger;

/**
 * Mock Lambda context.
 */
public class MockLambdaContext implements Context {

    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static LambdaLogger logger = new MockLambdaConsoleLogger();


    //-------------------------------------------------------------
    // Implementation - Context
    //-------------------------------------------------------------


    @Override
    public String getAwsRequestId() {
        return null;
    }


    @Override
    public String getLogGroupName() {
        return null;
    }


    @Override
    public String getLogStreamName() {
        return null;
    }


    @Override
    public String getFunctionName() {
        return null;
    }


    @Override
    public String getFunctionVersion() {
        return null;
    }


    @Override
    public String getInvokedFunctionArn() {
        return null;
    }


    @Override
    public CognitoIdentity getIdentity() {
        return null;
    }


    @Override
    public ClientContext getClientContext() {
        return null;
    }


    @Override
    public int getRemainingTimeInMillis() {
        return 0;
    }


    @Override
    public int getMemoryLimitInMB() {
        return 0;
    }


    @Override
    public LambdaLogger getLogger() {
        return logger;
    }
}
