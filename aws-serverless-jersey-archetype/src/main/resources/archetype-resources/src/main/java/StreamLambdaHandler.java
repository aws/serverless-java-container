package ${groupId};

import com.amazonaws.serverless.proxy.jersey.JerseyLambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.RequestStreamHandler;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import ${groupId}.resource.PingResource;


public class StreamLambdaHandler implements RequestStreamHandler {
    private static final ResourceConfig jerseyApplication = new ResourceConfig()
            // properties to speed up Jersey start time
            .property(ServerProperties.FEATURE_AUTO_DISCOVERY_DISABLE,true)
            .property(ServerProperties.WADL_FEATURE_DISABLE,true)
            .property(ServerProperties.METAINF_SERVICES_LOOKUP_DISABLE,true)
            .property(ServerProperties.BV_FEATURE_DISABLE,true)
            .property(ServerProperties.JSON_PROCESSING_FEATURE_DISABLE,true)
            .property(ServerProperties.MOXY_JSON_FEATURE_DISABLE,true)
            .register(PingResource.class)
            .register(JacksonFeature.class);
    private static final JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler
            = JerseyLambdaContainerHandler.getAwsProxyHandler(jerseyApplication);

    @Override
    public void handleRequest(InputStream inputStream, OutputStream outputStream, Context context)
            throws IOException {
        handler.proxyStream(inputStream, outputStream, context);
    }
}