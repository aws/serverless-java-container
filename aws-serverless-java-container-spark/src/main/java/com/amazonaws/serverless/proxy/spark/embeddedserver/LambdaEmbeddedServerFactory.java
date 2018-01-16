package com.amazonaws.serverless.proxy.spark.embeddedserver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
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
    @SuppressFBWarnings("ST_WRITE_TO_STATIC_FROM_INSTANCE_METHOD") // suppressing the warining as this constructor is only used for unit tests
    public LambdaEmbeddedServerFactory(LambdaEmbeddedServer server) {
        LambdaEmbeddedServerFactory.embeddedServer = server;
    }


    //-------------------------------------------------------------
    // Implementation - EmbeddedServerFactory
    //-------------------------------------------------------------


    @Override
    public EmbeddedServer create(Routes routes, StaticFilesConfiguration staticFilesConfiguration, boolean multipleHandlers) {
        if (embeddedServer == null) {
            LambdaEmbeddedServerFactory.embeddedServer = new LambdaEmbeddedServer(routes, staticFilesConfiguration, multipleHandlers);
        }

        return embeddedServer;
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public LambdaEmbeddedServer getServerInstance() {
        return embeddedServer;
    }
}
