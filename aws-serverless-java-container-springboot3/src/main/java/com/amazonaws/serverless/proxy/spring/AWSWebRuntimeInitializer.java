/*
 * Copyright 2024-2024 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.amazonaws.serverless.proxy.spring;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.boot.web.servlet.context.ServletWebServerApplicationContext;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.SmartLifecycle;
import org.springframework.context.support.GenericApplicationContext;
import org.springframework.core.env.Environment;
import org.springframework.util.StringUtils;

/**
 * Initializer to optionally start Custom Runtime to process web workloads.
 * Registered with META-INF/spring.factories
 * 
 * @author Dave Syer
 * @author Oleg Zhurakousky
 */
public class AWSWebRuntimeInitializer implements ApplicationContextInitializer<GenericApplicationContext> {

	private static Log logger = LogFactory.getLog(AWSWebRuntimeInitializer.class);

	@Override
	public void initialize(GenericApplicationContext context) {
		logger.info("AWS Environment: " + System.getenv());
		Environment environment = context.getEnvironment();
		if (logger.isDebugEnabled()) {
			logger.debug("AWS Environment: " + System.getenv());
		}
		
		if (context instanceof ServletWebServerApplicationContext && isCustomRuntime(environment)) {
			if (context.getBeanFactory().getBeanNamesForType(AWSWebRuntimeEventLoop.class, false, false).length == 0) {
				context.registerBean(StringUtils.uncapitalize(AWSWebRuntimeEventLoop.class.getSimpleName()),
						SmartLifecycle.class, () -> new AWSWebRuntimeEventLoop((ServletWebServerApplicationContext) context));
			}
		}
	}

	private boolean isCustomRuntime(Environment environment) {
		String handler = environment.getProperty("_HANDLER");
		if (StringUtils.hasText(handler)) {
			handler = handler.split(":")[0];
			logger.info("AWS Handler: " + handler);
			try {
				Thread.currentThread().getContextClassLoader().loadClass(handler);
			}
			catch (Exception e) {
				logger.debug("Will execute Lambda in Custom Runtime");
				return true;
			}
		}
		return false;
	}
}
