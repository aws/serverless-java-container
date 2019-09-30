package com.amazonaws.serverless.proxy.spring.embedded;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.boot.web.servlet.server.ServletWebServerFactory;
import org.springframework.core.Ordered;

import javax.servlet.ServletException;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ServerlessServletEmbeddedServerFactory implements ServletWebServerFactory, WebServer {
    private ServletContextInitializer[] initializers;
    private AwsLambdaServletContainerHandler handler;

    public ServerlessServletEmbeddedServerFactory() {
        super();
        handler = SpringBootLambdaContainerHandler.getInstance();
    }

    @Override
    public WebServer getWebServer(ServletContextInitializer... initializers) {
        this.initializers = initializers;
        for (ServletContextInitializer i : initializers) {
            try {
                if (handler.getServletContext() == null) {
                    throw new WebServerException("Attempting to initialize ServletEmbeddedWebServer without ServletContext in Handler", null);
                }
                i.onStartup(handler.getServletContext());
            } catch (ServletException e) {
                throw new WebServerException("Could not initialize Servlets", e);
            }
        }
        return this;
    }

    @Override
    public void start() throws WebServerException {

    }

    @Override
    public void stop() throws WebServerException {

    }

    @Override
    public int getPort() {
        return 0;
    }
}
