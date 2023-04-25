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
package com.amazonaws.serverless.proxy.spring.embedded;

import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.boot.autoconfigure.AutoConfigureOrder;
import org.springframework.boot.web.reactive.server.AbstractReactiveWebServerFactory;
import org.springframework.boot.web.server.WebServer;
import org.springframework.boot.web.server.WebServerException;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.HttpHandler;
import org.springframework.http.server.reactive.ServletHttpHandlerAdapter;

import jakarta.servlet.*;
import java.io.IOException;
import java.util.Enumeration;

@AutoConfigureOrder(Ordered.HIGHEST_PRECEDENCE)
public class ServerlessReactiveServletEmbeddedServerFactory extends AbstractReactiveWebServerFactory implements WebServer, Servlet {
    private ServletHttpHandlerAdapter handler;
    private ServletConfig config;
    static final String SERVLET_NAME = "com.amazonaws.serverless.proxy.spring.embedded.ServerlessReactiveEmbeddedServerFactory";
    static final String SERVLET_INFO = "ServerlessReactiveEmbeddedServerFactory";

    @Override
    @SuppressFBWarnings("MTIA_SUSPECT_SERVLET_INSTANCE_FIELD")
    public WebServer getWebServer(HttpHandler httpHandler) {
        handler = new ServletHttpHandlerAdapter(httpHandler);
        return this;
    }

    @Override
    public void start() throws WebServerException {
        // register this object as the main handler servlet with a mapping of /
        SpringBootLambdaContainerHandler
                .getInstance()
                .getServletContext()
                .addServlet(SERVLET_NAME, this)
                .addMapping("/");
        handler.init(new ServletAdapterConfig());
    }

    @Override
    public void stop() throws WebServerException {
        // nothing to do here.
    }

    @Override
    public void init(ServletConfig servletConfig) throws ServletException {
        config = servletConfig;
    }

    @Override
    public ServletConfig getServletConfig() {
        return config;
    }

    @Override
    public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {
        handler.service(servletRequest, servletResponse);
    }

    @Override
    public String getServletInfo() {
        return SERVLET_INFO;
    }

    @Override
    public void destroy() {

    }

    private static class ServletAdapterConfig implements ServletConfig {
        @Override
        public String getServletName() {
            return SERVLET_NAME;
        }

        @Override
        public ServletContext getServletContext() {
            return SpringBootLambdaContainerHandler.getInstance().getServletContext();
        }

        @Override
        public String getInitParameter(String s) {
            return null;
        }

        @Override
        public Enumeration<String> getInitParameterNames() {
            return null;
        }
    }
}
