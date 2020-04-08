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
package com.amazonaws.serverless.proxy.spark.embeddedserver;

import com.amazonaws.serverless.proxy.internal.testutils.Timer;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import spark.ExceptionMapper;
import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

public class LambdaEmbeddedServerFactory implements EmbeddedServerFactory {

    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static volatile LambdaEmbeddedServer embeddedServer;


    /**
     * Empty constructor, applications should always use this constructor.
     */
    public LambdaEmbeddedServerFactory() {}


    /**
     * Constructor used in unit tests to inject a mocked embedded server
     * @param server The mocked server
     */
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") // suppressing the warning as this constructor is only used for unit tests
    public LambdaEmbeddedServerFactory(LambdaEmbeddedServer server) {
        LambdaEmbeddedServerFactory.embeddedServer = server;
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServerFactory
    //-------------------------------------------------------------


    @Override
    public EmbeddedServer create(Routes routes, StaticFilesConfiguration staticFilesConfiguration, ExceptionMapper exceptionMapper, boolean multipleHandlers) {
        Timer.start("SPARK_SERVER_FACTORY_CREATE");
        if (embeddedServer == null) {
            LambdaEmbeddedServerFactory.embeddedServer = new LambdaEmbeddedServer(routes, staticFilesConfiguration, exceptionMapper, multipleHandlers);
        }
        Timer.stop("SPARK_SERVER_FACTORY_CREATE");
        return embeddedServer;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public LambdaEmbeddedServer getServerInstance() {
        return embeddedServer;
    }
}
