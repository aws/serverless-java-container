package com.amazonaws.serverless.runtime;

import com.amazonaws.services.lambda.runtime.Context;

import java.io.InputStream;

public class InvocationRequest {
    private InputStream event;
    private Context context;

    public InputStream getEvent() {
        return event;
    }

    public void setEvent(InputStream event) {
        this.event = event;
    }

    public Context getContext() {
        return context;
    }

    public void setContext(Context context) {
        this.context = context;
    }
}
