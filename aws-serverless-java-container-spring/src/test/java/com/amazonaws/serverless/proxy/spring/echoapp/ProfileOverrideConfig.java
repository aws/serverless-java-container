package com.amazonaws.serverless.proxy.spring.echoapp;

import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.context.annotation.PropertySource;

/**
 * Created by matthewcorey on 2/20/17.
 */
@Configuration
@Profile("override")
@PropertySource("classpath:/application-override.properties")
public class ProfileOverrideConfig {
}
