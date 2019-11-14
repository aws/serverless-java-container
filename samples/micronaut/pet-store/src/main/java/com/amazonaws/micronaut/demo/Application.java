package com.amazonaws.micronaut.demo;

import org.springframework.boot.autoconfigure.SpringBootApplication;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import io.micronaut.runtime.Micronaut;

@SpringBootApplication
public class Application {

    public static void main(String[] args) {
        Logger rootLogger = (ch.qos.logback.classic.Logger)org.slf4j.LoggerFactory.getLogger(org.slf4j.Logger.ROOT_LOGGER_NAME);
        rootLogger.setLevel(Level.TRACE);
        Micronaut.run(Application.class);
    }
}