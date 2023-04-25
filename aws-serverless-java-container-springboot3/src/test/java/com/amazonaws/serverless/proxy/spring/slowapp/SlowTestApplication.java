package com.amazonaws.serverless.proxy.spring.slowapp;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.stereotype.Component;

import java.time.Instant;

@SpringBootApplication(exclude = {
        org.springframework.boot.autoconfigure.security.reactive.ReactiveUserDetailsServiceAutoConfiguration.class,
        org.springframework.boot.autoconfigure.security.reactive.ReactiveSecurityAutoConfiguration.class
})
public class SlowTestApplication {

    @Component
    public static class SlowDownInit implements InitializingBean {
        public static final int INIT_SLEEP_TIME_MS = 13_000;

        @Override
        public void afterPropertiesSet() throws Exception {
            Thread.sleep(INIT_SLEEP_TIME_MS);
        }
    }
}
