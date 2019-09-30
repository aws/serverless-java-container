package com.amazonaws.serverless.proxy.spring.embedded;

import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import org.springframework.boot.context.embedded.AbstractEmbeddedServletContainerFactory;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;

import javax.servlet.ServletException;

/**
 * Implementation of SpringBoot's embedded container factory and servlet container. This replaces SpringBoot's default
 * embedded container and uses the {@link SpringBootLambdaContainerHandler} as a singleton to retrieve the current
 * {@link javax.servlet.ServletContext} and pass it to the array of {@link javax.servlet.ServletContainerInitializer}.
 */
public class ServerlessServletEmbeddedServerFactory extends AbstractEmbeddedServletContainerFactory implements EmbeddedServletContainer {
    private AwsLambdaServletContainerHandler handler;
    private ServletContextInitializer[] initializers;

    public ServerlessServletEmbeddedServerFactory() {
        super();
        handler = SpringBootLambdaContainerHandler.getInstance();
    }

    @Override
    public EmbeddedServletContainer getEmbeddedServletContainer(ServletContextInitializer... servletContextInitializers) {
        initializers = servletContextInitializers;

        return this;
    }

    @Override
    public void start() throws EmbeddedServletContainerException {
        for (ServletContextInitializer i : initializers) {
            try {
                if (handler.getServletContext() == null) {
                    throw new EmbeddedServletContainerException("Attempting to initialize ServletEmbeddedWebServer without ServletContext in Handler", null);
                }
                i.onStartup(handler.getServletContext());
            } catch (ServletException e) {
                throw new EmbeddedServletContainerException("Could not initialize Servlets", e);
            }
        }
    }

    ServletContextInitializer[] getInitializers() {
        return initializers;
    }

    @Override
    public void stop() throws EmbeddedServletContainerException {

    }
}
