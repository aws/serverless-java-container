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

import java.io.IOException;
import java.util.HashMap;

@JsonSerialize(using = HttpApiV2AuthorizerMap.HttpApiV2AuthorizerSerializer.class)
@JsonDeserialize(using = HttpApiV2AuthorizerMap.HttpApiV2AuthorizerDeserializer.class)
public class HttpApiV2AuthorizerMap extends HashMap<String, Object> {
    private static final String JWT_KEY = "jwt";
    private static final long serialVersionUID = 42L;

    public HttpApiV2JwtAuthorizer getJwtAuthorizer() {
        return (HttpApiV2JwtAuthorizer)get(JWT_KEY);
    }

    public boolean isJwt() {
        return containsKey(JWT_KEY);
    }

    public void putJwtAuthorizer(HttpApiV2JwtAuthorizer jwt) {
        put(JWT_KEY, jwt);
    }

    public static class HttpApiV2AuthorizerDeserializer extends StdDeserializer<HttpApiV2AuthorizerMap> {
        private static final long serialVersionUID = 42L;

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
        private static final long serialVersionUID = 42L;

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