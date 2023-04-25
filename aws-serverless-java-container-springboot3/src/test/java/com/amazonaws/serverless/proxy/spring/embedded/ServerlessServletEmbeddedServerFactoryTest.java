package com.amazonaws.serverless.proxy.spring.embedded;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import org.junit.jupiter.api.Test;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;

import static org.junit.jupiter.api.Assertions.fail;

public class ServerlessServletEmbeddedServerFactoryTest {
    private SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = new SpringBootLambdaContainerHandler<>(
            AwsProxyRequest.class,
            AwsProxyResponse.class,
            new AwsProxyHttpServletRequestReader(),
            new AwsProxyHttpServletResponseWriter(),
            new AwsProxySecurityContextWriter(),
            new AwsProxyExceptionHandler(),
            null,
            new InitializationWrapper(),
            WebApplicationType.REACTIVE
    );

    public ServerlessServletEmbeddedServerFactoryTest() throws ContainerInitializationException {
    }

    @Test
    void getWebServer_callsInitializers() {
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
