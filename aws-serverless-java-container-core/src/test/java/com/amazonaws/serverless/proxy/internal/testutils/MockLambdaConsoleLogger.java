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

import com.amazonaws.services.lambda.runtime.LambdaLogger;

import java.nio.charset.Charset;


/**
 * Mock LambdaLogger object that prints output to the console
 */
public class MockLambdaConsoleLogger implements LambdaLogger {

    //-------------------------------------------------------------
    // Implementation - LambdaLogger
    //-------------------------------------------------------------

    @Override
    public void log(String s) {
        System.out.println(s);
    }


    @Override
    public void log(byte[] bytes) {
        System.out.println(new String(bytes, Charset.defaultCharset()));
    }
}
