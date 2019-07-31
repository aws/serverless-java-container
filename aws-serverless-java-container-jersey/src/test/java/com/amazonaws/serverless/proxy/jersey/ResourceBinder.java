package com.amazonaws.serverless.proxy.jersey;

import org.glassfish.jersey.internal.inject.AbstractBinder;

import javax.inject.Singleton;

public class ResourceBinder extends AbstractBinder {
    @Override
    protected void configure() {
        bind(new JerseyDependency()).to(JerseyDependency.class).in(Singleton.class);
    }
}
