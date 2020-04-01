package com.amazonaws.serverless.proxy.model;

public class HttpApiV2ProxyRequestContext {
    private String accountId;
    private String apiId;
    private String domainName;
    private String domainPrefix;
    private String requestId;
    private String routeKey;
    private String stage;
    private String time;
    private long timeEpoch;

    private HttpApiV2HttpContext http;
    private HttpApiV2AuthorizerMap authorizer;

    public String getAccountId() {
        return accountId;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public String getApiId() {
        return apiId;
    }

    public void setApiId(String apiId) {
        this.apiId = apiId;
    }

    public String getDomainName() {
        return domainName;
    }

    public void setDomainName(String domainName) {
        this.domainName = domainName;
    }

    public String getDomainPrefix() {
        return domainPrefix;
    }

    public void setDomainPrefix(String domainPrefix) {
        this.domainPrefix = domainPrefix;
    }

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getRouteKey() {
        return routeKey;
    }

    public void setRouteKey(String routeKey) {
        this.routeKey = routeKey;
    }

    public String getStage() {
        return stage;
    }

    public void setStage(String stage) {
        this.stage = stage;
    }

    public String getTime() {
        return time;
    }

    public void setTime(String time) {
        this.time = time;
    }

    public long getTimeEpoch() {
        return timeEpoch;
    }

    public void setTimeEpoch(long timeEpoch) {
        this.timeEpoch = timeEpoch;
    }

    public HttpApiV2HttpContext getHttp() {
        return http;
    }

    public void setHttp(HttpApiV2HttpContext http) {
        this.http = http;
    }

    public HttpApiV2AuthorizerMap getAuthorizer() {
        return authorizer;
    }

    public void setAuthorizer(HttpApiV2AuthorizerMap authorizer) {
        this.authorizer = authorizer;
    }

}
