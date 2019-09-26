package com.amazonaws.servlerss.proxy.spring.embedded;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.SyncInitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.embedded.ServerlessServletEmbeddedServerFactory;
import org.junit.Test;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import static org.junit.Assert.fail;

public class ServerlessServletEmbeddedServerFactoryTest {
    private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = new SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse>(
            AwsProxyRequest.class,
            AwsProxyResponse.class,
            new AwsProxyHttpServletRequestReader(),
            new AwsProxyHttpServletResponseWriter(),
            new AwsProxySecurityContextWriter(),
            new AwsProxyExceptionHandler(),
            null,
            new SyncInitializationWrapper()
    );

    public ServerlessServletEmbeddedServerFactoryTest() throws ContainerInitializationException {
    }

    @Test
    public void getWebServer_callsInitializers() {
        ServerlessServletEmbeddedServerFactory factory = new ServerlessServletEmbeddedServerFactory();
        factory.getWebServer(new ServletContextInitializer() {
            @Override
            public void onStartup(ServletContext servletContext) throws ServletException {
                if (servletContext == null) {
                    fail("Null servlet context");
                }
            }
        });
    }
}
