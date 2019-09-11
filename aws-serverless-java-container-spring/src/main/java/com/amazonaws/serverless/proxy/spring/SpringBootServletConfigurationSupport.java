package com.amazonaws.serverless.proxy.spring;

import org.springframework.boot.context.embedded.WebApplicationContextServletContextAwareProcessor;
import org.springframework.web.context.ConfigurableWebApplicationContext;


public class SpringBootServletConfigurationSupport extends WebApplicationContextServletContextAwareProcessor {
    public SpringBootServletConfigurationSupport(ConfigurableWebApplicationContext webApplicationContext) {
        super(webApplicationContext);
        webApplicationContext.setServletContext(SpringBootLambdaContainerHandler.getInstance().getServletContext());
    }
}
