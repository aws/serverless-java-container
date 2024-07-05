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

import com.amazonaws.serverless.proxy.model.*;
import org.springframework.aot.generate.GenerationContext;
import org.springframework.aot.hint.MemberCategory;
import org.springframework.aot.hint.RuntimeHints;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotContribution;
import org.springframework.beans.factory.aot.BeanFactoryInitializationAotProcessor;
import org.springframework.beans.factory.aot.BeanFactoryInitializationCode;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;

import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.fasterxml.jackson.core.JsonToken;

/**
 * AOT Initialization processor required to register reflective hints for GraalVM.
 * This is necessary to ensure proper JSON serialization/deserialization.
 * It is registered with META-INF/spring/aot.factories
 *
 * @author Oleg Zhurakousky
 */
public class AwsSpringAotTypesProcessor implements BeanFactoryInitializationAotProcessor {

	@Override
	public BeanFactoryInitializationAotContribution processAheadOfTime(ConfigurableListableBeanFactory beanFactory) {
		return new ReflectiveProcessorBeanFactoryInitializationAotContribution();
	}

	private static final class ReflectiveProcessorBeanFactoryInitializationAotContribution implements BeanFactoryInitializationAotContribution {
		@Override
		public void applyTo(GenerationContext generationContext, BeanFactoryInitializationCode beanFactoryInitializationCode) {
			RuntimeHints runtimeHints = generationContext.getRuntimeHints();
			// known static types
			
			runtimeHints.reflection().registerType(AwsProxyRequest.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(AwsProxyResponse.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(SingleValueHeaders.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(JsonToken.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(MultiValuedTreeMap.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(Headers.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(AwsProxyRequestContext.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(ApiGatewayRequestIdentity.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES);
			runtimeHints.reflection().registerType(AwsHttpServletResponse.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS, 
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2ProxyRequest.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2HttpContext.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2ProxyRequestContext.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2AuthorizerMap.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2AuthorizerMap.HttpApiV2AuthorizerDeserializer.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
			runtimeHints.reflection().registerType(HttpApiV2AuthorizerMap.HttpApiV2AuthorizerSerializer.class,
					MemberCategory.INVOKE_PUBLIC_METHODS, MemberCategory.INVOKE_PUBLIC_CONSTRUCTORS,
					MemberCategory.DECLARED_FIELDS, MemberCategory.DECLARED_CLASSES, MemberCategory.INTROSPECT_DECLARED_METHODS);
		}

	}
}
