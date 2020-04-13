package com.amazonaws.serverless.runtime;

public class RuntimeClientException extends Exception {
    private boolean retriable;

    public RuntimeClientException(String msg, Throwable e) {
        this(msg, e, false);
    }

    public RuntimeClientException(String msg, Throwable e, boolean retriable) {
        super(msg, e);
        this.retriable = retriable;
    }

    public boolean isRetriable() {
        return retriable;
    }

    public void setRetriable(boolean retriable) {
        this.retriable = retriable;
    }
}
