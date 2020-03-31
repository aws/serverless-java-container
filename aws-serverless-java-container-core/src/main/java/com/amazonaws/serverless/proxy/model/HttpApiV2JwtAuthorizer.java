package com.amazonaws.serverless.proxy.model;

import java.util.List;
import java.util.Map;

public class HttpApiV2JwtAuthorizer {
    private Map<String, String> claims;
    private List<String> scopes;

    public Map<String, String> getClaims() {
        return claims;
    }

    public void setClaims(Map<String, String> claims) {
        this.claims = claims;
    }

    public List<String> getScopes() {
        return scopes;
    }

    public void setScopes(List<String> scopes) {
        this.scopes = scopes;
    }
}
