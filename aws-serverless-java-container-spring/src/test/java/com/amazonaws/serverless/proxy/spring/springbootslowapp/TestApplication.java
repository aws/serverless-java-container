package com.amazonaws.serverless.proxy.spring.springbootslowapp;


import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.support.SpringBootServletInitializer;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.stereotype.Component;

// Need to explicitly exclude security because of some bizarre witchcraft inside SpringBoot that
// enables it even when I don't ask for it - this is only true when I test against spring-webmvc 4.3.x
@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.SecurityAutoConfiguration.class
})
@ComponentScan(basePackages="com.amazonaws.serverless.proxy.spring.springbootslowapp")
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
