package com.amazonaws.serverless.proxy.spring.springbootslowapp;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.PropertySource;
import org.springframework.stereotype.Component;


@SpringBootApplication
@ComponentScan(basePackages = "com.amazonaws.serverless.proxy.spring.springbootslowapp")
@PropertySource("classpath:boot-application.properties")
public class TestApplication extends SpringBootServletInitializer {
    @Component
    public static class SlowDownInit implements InitializingBean {
        public static final int INIT_SLEEP_TIME_MS = 13_000;

        @Override
        public void afterPropertiesSet() throws Exception {
            Thread.sleep(INIT_SLEEP_TIME_MS);
        }
    }
}
