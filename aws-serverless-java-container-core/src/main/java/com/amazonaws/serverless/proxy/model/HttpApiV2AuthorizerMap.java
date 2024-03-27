/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
import com.fasterxml.jackson.databind.type.TypeFactory;

import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@JsonSerialize(using = HttpApiV2AuthorizerMap.HttpApiV2AuthorizerSerializer.class)
@JsonDeserialize(using = HttpApiV2AuthorizerMap.HttpApiV2AuthorizerDeserializer.class)
public class HttpApiV2AuthorizerMap extends HashMap<String, Object> {
    private static final String JWT_KEY = "jwt";
    private static final String LAMBDA_KEY = "lambda";
    private static final String IAM_KEY = "iam";
    private static final long serialVersionUID = 42L;

    public HttpApiV2JwtAuthorizer getJwtAuthorizer() {
        return (HttpApiV2JwtAuthorizer) get(JWT_KEY);
    }

    public Map<String, Object> getLambdaAuthorizerContext() {
        return (Map<String, Object>) get(LAMBDA_KEY);
    }

    public HttpApiV2IamAuthorizer getIamAuthorizer() {
        return (HttpApiV2IamAuthorizer) get(IAM_KEY);
    }

    public boolean isJwt() {
        return containsKey(JWT_KEY);
    }

    public boolean isLambda() {
        return containsKey(LAMBDA_KEY);
    }

    public boolean isIam() {
        return containsKey(IAM_KEY);
    }

    public void putJwtAuthorizer(HttpApiV2JwtAuthorizer jwt) {
        put(JWT_KEY, jwt);
    }

    public void putIamAuthorizer(HttpApiV2IamAuthorizer iam) {
        put(IAM_KEY, iam);
    }

    public static class HttpApiV2AuthorizerDeserializer extends StdDeserializer<HttpApiV2AuthorizerMap> {
        private static final long serialVersionUID = 42L;

        public HttpApiV2AuthorizerDeserializer() {
            super(HttpApiV2AuthorizerMap.class);
        }

        @Override
        public HttpApiV2AuthorizerMap deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
                throws IOException, JsonProcessingException {
            HttpApiV2AuthorizerMap map = new HttpApiV2AuthorizerMap();
            JsonNode node = jsonParser.getCodec().readTree(jsonParser);
            if (node.has(JWT_KEY)) {
                HttpApiV2JwtAuthorizer authorizer = LambdaContainerHandler.getObjectMapper()
                        .treeToValue(node.get(JWT_KEY), HttpApiV2JwtAuthorizer.class);
                map.putJwtAuthorizer(authorizer);
            }
            if (node.has(LAMBDA_KEY)) {
                Map<String, Object> context = LambdaContainerHandler.getObjectMapper().treeToValue(node.get(LAMBDA_KEY),
                        TypeFactory.defaultInstance().constructMapType(HashMap.class, String.class, Object.class));
                map.put(LAMBDA_KEY, context);
            }
            if (node.has(IAM_KEY)) {
                HttpApiV2IamAuthorizer iam_authorizer = LambdaContainerHandler.getObjectMapper()
                        .treeToValue(node.get(IAM_KEY), HttpApiV2IamAuthorizer.class);
                map.putIamAuthorizer(iam_authorizer);
            }
            // we ignore other, unknown values
            return map;
        }
    }

    public static class HttpApiV2AuthorizerSerializer extends StdSerializer<HttpApiV2AuthorizerMap> {
        private static final long serialVersionUID = 42L;

        public HttpApiV2AuthorizerSerializer() {
            super(HttpApiV2AuthorizerMap.class);
        }

        @Override
        public void serialize(HttpApiV2AuthorizerMap httpApiV2AuthorizerMap, JsonGenerator jsonGenerator,
                SerializerProvider serializerProvider) throws IOException {
            jsonGenerator.writeStartObject();
            if (httpApiV2AuthorizerMap.isJwt()) {
                jsonGenerator.writeObjectField(JWT_KEY, httpApiV2AuthorizerMap.getJwtAuthorizer());
            }
            if (httpApiV2AuthorizerMap.isLambda()) {
                jsonGenerator.writeObjectField(LAMBDA_KEY, httpApiV2AuthorizerMap.getLambdaAuthorizerContext());
            }
            if (httpApiV2AuthorizerMap.isIam()) {
                jsonGenerator.writeObjectField(IAM_KEY, httpApiV2AuthorizerMap.get(IAM_KEY));
            }
            jsonGenerator.writeEndObject();
        }
    }
}