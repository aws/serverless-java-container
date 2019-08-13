/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.jersey;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.Test;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;

/**
 * Test that one can access the Jersey injection manager
 */
public class JerseyInjectionTest {

    // Test resource binder
    private static class ResourceBinder extends AbstractBinder {

        @Override
        protected void configure() {
            bind(new JerseyInjectionTest()).to(JerseyInjectionTest.class).in(Singleton.class);
        }

    }

    private static ResourceConfig app = new ResourceConfig().register(MultiPartFeature.class)
                                                            .register(new ResourceBinder());

    private static JerseyLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = JerseyLambdaContainerHandler.getAwsProxyHandler(
            app);

    @Test
    public void can_get_injected_resources() throws Exception {

        JerseyInjectionTest instance1 = handler.getInjectionManager().getInstance(JerseyInjectionTest.class);
        assertNotNull(instance1);

        JerseyInjectionTest instance2 = handler.getInjectionManager().getInstance(JerseyInjectionTest.class);
        assertEquals(instance1, instance2);

    }
}
