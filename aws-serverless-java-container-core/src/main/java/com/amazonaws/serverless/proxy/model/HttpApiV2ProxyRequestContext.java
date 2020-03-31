package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.fasterxml.jackson.core.JsonGenerator;
import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.SerializerProvider;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;
import com.fasterxml.jackson.databind.ser.std.StdSerializer;

import java.io.IOException;
import java.util.HashMap;

public class HttpApiV2ProxyRequestContext {
    private static final String JWT_KEY = "jwt";

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

    @JsonSerialize(using = HttpApiV2AuthorizerSerializer.class)
    @JsonDeserialize(using = HttpApiV2AuthorizerDeserializer.class)
    public static class HttpApiV2AuthorizerMap extends HashMap<String, Object> {
        public HttpApiV2JwtAuthorizer getJwtAuthorizer() {
            return (HttpApiV2JwtAuthorizer)get(JWT_KEY);
        }

        public boolean isJwt() {
            return containsKey(JWT_KEY);
        }

        public void putJwtAuthorizer(HttpApiV2JwtAuthorizer jwt) {
            put(JWT_KEY, jwt);
        }
    }

    public static class HttpApiV2AuthorizerDeserializer extends StdDeserializer<HttpApiV2AuthorizerMap> {

        public HttpApiV2AuthorizerDeserializer() {
            super(HttpApiV2AuthorizerMap.class);
        }

        @Override
        public HttpApiV2AuthorizerMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext) throws IOException, JsonProcessingException {
            HttpApiV2AuthorizerMap map = new HttpApiV2AuthorizerMap();
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            if (node.get(JWT_KEY) != null) {
                HttpApiV2JwtAuthorizer authorizer = LambdaContainerHandler.getObjectMapper().treeToValue(node.get(JWT_KEY), HttpApiV2JwtAuthorizer.class);
                map.putJwtAuthorizer(authorizer);
            }
            // we ignore other, unknown values
            return map;
        }
    }

    public static class HttpApiV2AuthorizerSerializer extends StdSerializer<HttpApiV2AuthorizerMap> {

        public HttpApiV2AuthorizerSerializer() {
            super(HttpApiV2AuthorizerMap.class);
        }

        @Override
        public void serialize(HttpApiV2AuthorizerMap httpApiV2AuthorizerMap, JsonGenerator jsonGenerator, SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            if (httpApiV2AuthorizerMap.isJwt()) {
                jsonGenerator.writeObjectField(JWT_KEY, httpApiV2AuthorizerMap.getJwtAuthorizer());
            }
            jsonGenerator.writeEndObject();
        }
    }
}
