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
package com.amazonaws.serverless.proxy.internal.jaxrs;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.SecurityContext;
import java.nio.charset.StandardCharsets;
import java.security.Principal;
import java.util.Base64;

public class AwsHttpApiV2SecurityContext implements SecurityContext {
    public static final String AUTH_SCHEME_JWT = "JWT";

    private static Logger log = LoggerFactory.getLogger(AwsHttpApiV2SecurityContext.class);

    private Context lambdaContext;
    private HttpApiV2ProxyRequest event;

    public AwsHttpApiV2SecurityContext(final Context lambdaCtx, final HttpApiV2ProxyRequest request) {
        lambdaContext = lambdaCtx;
        event = request;
    }

    @Override
    public Principal getUserPrincipal() {
        if (getAuthenticationScheme() == null || !event.getHeaders().containsKey(HttpHeaders.AUTHORIZATION)) {
            return null;
        }

        String authValue = event.getHeaders().get(HttpHeaders.AUTHORIZATION);
        if (authValue.startsWith("Bearer ")) {
            authValue = authValue.replace("Bearer ", "");
        }
        String[] parts = authValue.split("\\.");
        if (parts.length != 3) {
            log.warn("Could not parse JWT token for requestId: " + SecurityUtils.crlf(event.getRequestContext().getRequestId()));
            return null;
        }
        String decodedBody = new String(Base64.getMimeDecoder().decode(parts[1]), StandardCharsets.UTF_8);
        try {
            JsonNode parsedBody = LambdaContainerHandler.getObjectMapper().readTree(decodedBody);
            if (!parsedBody.isObject() && parsedBody.has("sub")) {
                log.debug("Could not find \"sub\" field in JWT body for requestId: " + SecurityUtils.crlf(event.getRequestContext().getRequestId()));
                return null;
            }
            String subject = parsedBody.get("sub").asText();
            return (() -> {
                return subject;
            });
        } catch (JsonProcessingException e) {
            log.error("Error while attempting to parse JWT body for requestId: " + SecurityUtils.crlf(event.getRequestContext().getRequestId()), e);
            return null;
        }

    }

    @Override
    public boolean isUserInRole(String s) {
        if (getAuthenticationScheme() == null) {
            return false;
        }

        return event.getRequestContext().getAuthorizer().getJwtAuthorizer().getScopes().contains(s) ||
                event.getRequestContext().getAuthorizer().getJwtAuthorizer().getClaims().containsKey(s);

    }

    @Override
    public boolean isSecure() {
        return getAuthenticationScheme() != null;
    }

    @Override
    public String getAuthenticationScheme() {
        if (event.getRequestContext().getAuthorizer() == null) {
            return null;
        }
        if (event.getRequestContext().getAuthorizer().isJwt()) {
            return AUTH_SCHEME_JWT;
        }
        return null;
    }
}
