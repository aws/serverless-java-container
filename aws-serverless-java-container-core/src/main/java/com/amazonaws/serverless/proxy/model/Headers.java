package com.amazonaws.serverless.proxy.model;

public class Headers extends MultiValuedTreeMap<String, String> {

    private static final long serialVersionUID = 42L;

    public Headers() {
        super(String.CASE_INSENSITIVE_ORDER);
    }
}
