package com.amazonaws.serverless.proxy.spring;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.context.ConfigurableApplicationContext;

public class AWSWebRuntimeTests {
	
	@AfterEach
	public void after() {
		System.clearProperty("_HANDLER");
	}

	@Test
    public void testWebRuntimeInitialization() throws Exception {
    	try (ConfigurableApplicationContext context = SpringApplication.run(EmptyApplication.class);) {
    		assertFalse(context.getBeansOfType(AWSWebRuntimeEventLoop.class).size() > 0);
    	}
    	System.setProperty("_HANDLER", "foo");
    	AWSWebRuntimeEventLoop loop = null;
    	try (ConfigurableApplicationContext context = SpringApplication.run(EmptyApplication.class);) {
    		Thread.sleep(100);
    		assertTrue(context.getBeansOfType(AWSWebRuntimeEventLoop.class).size() > 0);
    		loop = context.getBean(AWSWebRuntimeEventLoop.class);
    		assertTrue(loop.isRunning());
    	}
    	assertFalse(loop.isRunning());
    }
	
	@EnableAutoConfiguration
    private static class EmptyApplication {
    	
    }
}
