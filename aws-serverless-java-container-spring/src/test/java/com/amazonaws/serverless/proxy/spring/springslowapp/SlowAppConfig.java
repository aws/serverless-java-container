package com.amazonaws.serverless.proxy.spring.springslowapp;

import org.springframework.beans.factory.InitializingBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.stereotype.Component;


@Configuration
@Import({MessageController.class})
public class SlowAppConfig {

    @Component
    public static class SlowDownInit implements InitializingBean {
        public static final int INIT_SLEEP_TIME_MS = 13_000;

        @Override
        public void afterPropertiesSet() throws Exception {
            Thread.sleep(INIT_SLEEP_TIME_MS);
        }
    }
}
