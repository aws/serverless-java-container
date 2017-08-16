package com.amazonaws.serverless.proxy.spark.embeddedserver;

import spark.embeddedserver.EmbeddedServer;
import spark.embeddedserver.EmbeddedServerFactory;
import spark.route.Routes;
import spark.staticfiles.StaticFilesConfiguration;

public class LambdaEmbeddedServerFactory implements EmbeddedServerFactory {

    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static LambdaEmbeddedServer embeddedServer;


    /**
     * Empty constructor, applications should always use this constructor.
     */
    public LambdaEmbeddedServerFactory() {}


    /**
     * Constructor used in unit tests to inject a mocked embedded server
     * @param server The mocked server
     */
    public LambdaEmbeddedServerFactory(LambdaEmbeddedServer server) {
        embeddedServer = server;
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServerFactory
    //-------------------------------------------------------------


    @Override
    public EmbeddedServer create(Routes routes, StaticFilesConfiguration staticFilesConfiguration, boolean multipleHandlers) {
        if (embeddedServer == null) {
            embeddedServer = new LambdaEmbeddedServer(routes, staticFilesConfiguration, multipleHandlers);
        }

        return embeddedServer;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public static LambdaEmbeddedServer getServerInstance() {
        return embeddedServer;
    }
}
