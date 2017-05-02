package com.amazonaws.serverless.proxy.internal.serialization;


import com.amazonaws.serverless.proxy.internal.model.CognitoAuthorizerClaims;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.deser.std.StdDeserializer;

import java.io.IOException;


/**
 * Created by bulianis on 5/2/17.
 */
public class CognitoAuthorizerClaimsDeserializer extends JsonDeserializer<CognitoAuthorizerClaims> {
    @Override
    public CognitoAuthorizerClaims deserialize(JsonParser jsonParser, DeserializationContext deserializationContext)
            throws IOException, JsonProcessingException {
        CognitoAuthorizerClaims output = new CognitoAuthorizerClaims();
        JsonNode node = jsonParser.getCodec().readTree(jsonParser);

        output.setSubject(node.get("sub").asText());
        output.setUsername(node.get("cognito:username").asText());
        output.setIssuer(node.get("iss").asText());
        output.setAuthTime(node.get("auth_time").asLong());
        output.setAudience(node.get("aud").asText());
        output.setExpiration(node.get("exp").asText());
        output.setTokenUse(node.get("token_use").asText());
        output.setIssuedAt(node.get("iat").asText());

        if (node.get("email") != null) {
            output.setEmailVerified(node.get("email_verified").asBoolean());
            output.setEmail(node.get("email").asText());
        }

        if (node.get("phone_number")  != null) {
            output.setPhoneNumber(node.get("phone_number").asText());
            output.setPhoneNumberVerified(node.get("phone_number_verified").asBoolean());
        }

        return output;
    }
}
