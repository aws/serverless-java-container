/*
 * Copyright 2016 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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
package com.amazonaws.serverless.proxy.internal.testutils;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.*;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.hc.core5.http.ContentType;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.client5.http.entity.mime.MultipartEntityBuilder;
import org.apache.hc.client5.http.entity.mime.ByteArrayBody;
import org.apache.hc.client5.http.entity.mime.StringBody;

import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.*;


/**
 * Request builder object. This is used by unit proxy to quickly create an AWS_PROXY request object
 */
public class AwsProxyRequestBuilder {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private AwsProxyRequest request;
    private MultipartEntityBuilder multipartBuilder;
    
    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    public AwsProxyRequestBuilder() {
        this(null, null);
    }


    public AwsProxyRequestBuilder(String path) {
        this(path, null);
    }

    public AwsProxyRequestBuilder(AwsProxyRequest req) {
        request = req;
    }


    public AwsProxyRequestBuilder(String path, String httpMethod) {
        this.request = new AwsProxyRequest();
        this.request.setMultiValueHeaders(new Headers());// avoid NPE
        this.request.setHeaders(new SingleValueHeaders());
        this.request.setHttpMethod(httpMethod);
        this.request.setPath(path);
        this.request.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        this.request.setRequestContext(new AwsProxyRequestContext());
        this.request.getRequestContext().setRequestId(UUID.randomUUID().toString());
        this.request.getRequestContext().setExtendedRequestId(UUID.randomUUID().toString());
        this.request.getRequestContext().setStage("test");
        this.request.getRequestContext().setProtocol("HTTP/1.1");
        this.request.getRequestContext().setRequestTimeEpoch(System.currentTimeMillis());
        ApiGatewayRequestIdentity identity = new ApiGatewayRequestIdentity();
        identity.setSourceIp("127.0.0.1");
        this.request.getRequestContext().setIdentity(identity);
    }

        //-------------------------------------------------------------
    // Methods - Public
    //-------------------------------------------------------------

    public AwsProxyRequestBuilder alb() {
		/*
		 * This method sets up the requestContext to look like an ALB request and also
		 * re-encodes URL query params, since ALBs do not decode them. This now returns
		 * a new AwsProxyRequestBuilder with the new query param state, so the original
		 * builder maintains the original configured state and can be then be reused in
		 * further unit tests. For now the simplest way to accomplish a deep copy is by
		 * serializing to JSON then deserializing.
		 */
    	
    	ObjectMapper objectMapper = new ObjectMapper();
    	AwsProxyRequest albRequest = null;
    	try {
    		String json = objectMapper.writeValueAsString(this.request);
    		albRequest = objectMapper.readValue(json, AwsProxyRequest.class);
    	} catch (JsonProcessingException jpe) {
    		throw new RuntimeException(jpe);
    	}
    	
        if (albRequest.getRequestContext() == null) {
        	albRequest.setRequestContext(new AwsProxyRequestContext());
        }
        albRequest.getRequestContext().setElb(new AlbContext());
        albRequest.getRequestContext().getElb().setTargetGroupArn(
                "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/lambda-target/d6190d154bc908a5"
        );

        // ALB does not decode query string parameters so we re-encode them all
        if (albRequest.getMultiValueQueryStringParameters() != null) {
            MultiValuedTreeMap<String, String> newQs = new MultiValuedTreeMap<>();
            for (Map.Entry<String, List<String>> e : albRequest.getMultiValueQueryStringParameters().entrySet()) {
                for (String v : e.getValue()) {
                    try {
                        // this is a terrible hack. In our Spring tests we use the comma as a control character for lists
                        // this is allowed by the HTTP specs although not recommended.
                        String key = URLEncoder.encode(e.getKey(), "UTF-8").replaceAll("%2C", ",");
                        String value = URLEncoder.encode(v, "UTF-8").replaceAll("%2C", ",");
                        newQs.add(key, value);
                    } catch (UnsupportedEncodingException ex) {
                        throw new RuntimeException("Could not encode query string parameters: " + e.getKey() + "=" + v, ex);
                    }
                }
            }
            albRequest.setMultiValueQueryStringParameters(newQs);
        }
        return new AwsProxyRequestBuilder(albRequest);
    }

    public AwsProxyRequestBuilder stage(String stageName) {
        this.request.getRequestContext().setStage(stageName);
        return this;
    }

    public AwsProxyRequestBuilder method(String httpMethod) {
        this.request.setHttpMethod(httpMethod);
        return this;
    }


    public AwsProxyRequestBuilder path(String path) {
        this.request.setPath(path);
        return this;
    }


    public AwsProxyRequestBuilder json() {
        return this.header(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_JSON);
    }


    public AwsProxyRequestBuilder form(String key, String value) {
        if (key == null || value == null) {
            throw new IllegalArgumentException("form() does not support null key or value");
        }
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        request.getMultiValueHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        String body = request.getBody();
        if (body == null) {
            body = "";
        }
        // URL-encode key and value to form expected body of a form post
        try {
            body += (body.equals("") ? "" : "&") + URLEncoder.encode(key, "UTF-8") + "=" + URLEncoder.encode(value, "UTF-8");
        } catch (UnsupportedEncodingException ex) {
            throw new RuntimeException("Could not encode form parameter: " + key + "=" + value, ex);
        }
        request.setBody(body);
        return this;
    }

    public AwsProxyRequestBuilder formFilePart(String fieldName, String fileName, byte[] content) throws IOException {
        if (multipartBuilder == null) {
            multipartBuilder = MultipartEntityBuilder.create();
        }
        multipartBuilder.addPart(fieldName, new ByteArrayBody(content, fileName));
        buildMultipartBody();
        return this;
    }

    public AwsProxyRequestBuilder formTextFieldPart(String fieldName, String fieldValue)
            throws IOException {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        if (multipartBuilder == null) {
            multipartBuilder = MultipartEntityBuilder.create();
        }
        multipartBuilder.addPart(fieldName, new StringBody(fieldValue, ContentType.TEXT_PLAIN));
        buildMultipartBody();
        return this;
    }

    private void buildMultipartBody()
            throws IOException {
        HttpEntity bodyEntity = multipartBuilder.build();
        InputStream bodyStream = bodyEntity.getContent();
        byte[] buffer = new byte[bodyStream.available()];
        IOUtils.readFully(bodyStream, buffer);
        byte[] finalBuffer = new byte[buffer.length + 1];
        byte[] newLineBytes = "\n\n".getBytes(LambdaContainerHandler.getContainerConfig().getDefaultContentCharset());
        System.arraycopy(newLineBytes, 0, finalBuffer, 0, newLineBytes.length);
        System.arraycopy(buffer, 0, finalBuffer, newLineBytes.length - 1, buffer.length);
        request.setBody(Base64.getMimeEncoder().encodeToString(finalBuffer));
        request.setIsBase64Encoded(true);
        this.request.setMultiValueHeaders(new Headers());
        header(HttpHeaders.CONTENT_TYPE, bodyEntity.getContentType());
        header(HttpHeaders.CONTENT_LENGTH, bodyEntity.getContentLength() + "");
    }


    public AwsProxyRequestBuilder header(String key, String value) {
        if (this.request.getMultiValueHeaders() == null) {
            this.request.setMultiValueHeaders(new Headers());
        }

        this.request.getMultiValueHeaders().add(key, value);
        return this;
    }

    public AwsProxyRequestBuilder multiValueHeaders(Headers h) {
        this.request.setMultiValueHeaders(h);
        return this;
    }

    public AwsProxyRequestBuilder multiValueQueryString(MultiValuedTreeMap<String, String> params) {
        this.request.setMultiValueQueryStringParameters(params);
        return this;
    }

    public AwsProxyRequestBuilder queryString(String key, String value) {
        if (this.request.getMultiValueQueryStringParameters() == null) {
            this.request.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        }

        this.request.getMultiValueQueryStringParameters().add(key, value);
        return this;
    }

    public AwsProxyRequestBuilder body(String body) {
        this.request.setBody(body);
        return this;
    }

    public AwsProxyRequestBuilder nullBody() {
        this.request.setBody(null);
        return this;
    }

    public AwsProxyRequestBuilder body(Object body) {
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).startsWith(MediaType.APPLICATION_JSON)) {
            try {
                return body(LambdaContainerHandler.getObjectMapper().writeValueAsString(body));
            } catch (JsonProcessingException e) {
                throw new UnsupportedOperationException("Could not serialize object: " + e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported content type in request");
        }
    }

    public AwsProxyRequestBuilder apiId(String id) {
        if (request.getRequestContext() == null) {
            request.setRequestContext(new AwsProxyRequestContext());
        }
        request.getRequestContext().setApiId(id);
        return this;
    }

    public AwsProxyRequestBuilder binaryBody(InputStream is)
            throws IOException {
        this.request.setIsBase64Encoded(true);
        return body(Base64.getMimeEncoder().encodeToString(IOUtils.toByteArray(is)));
    }


    public AwsProxyRequestBuilder authorizerPrincipal(String principal) {
        if (this.request.getRequestSource() == RequestSource.API_GATEWAY) {
            if (this.request.getRequestContext().getAuthorizer() == null) {
                this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
            }
            this.request.getRequestContext().getAuthorizer().setPrincipalId(principal);
            if (this.request.getRequestContext().getAuthorizer().getClaims() == null) {
                this.request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
            }
            this.request.getRequestContext().getAuthorizer().getClaims().setSubject(principal);
        }
        if (this.request.getRequestSource() == RequestSource.ALB) {
            header("x-amzn-oidc-identity", principal);
            try {
                header(
                        "x-amzn-oidc-accesstoken",
                        Base64.getMimeEncoder().encodeToString(
                                "test-token".getBytes(ContainerConfig.DEFAULT_CONTENT_CHARSET)
                        )
                );
            } catch (UnsupportedEncodingException e) {
                throw new RuntimeException(e);
            }
        }
        return this;
    }

    public AwsProxyRequestBuilder authorizerContextValue(String key, String value) {
        if (this.request.getRequestContext().getAuthorizer() == null) {
            this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        }
        this.request.getRequestContext().getAuthorizer().setContextValue(key, value);
        return this;
    }


    public AwsProxyRequestBuilder cognitoUserPool(String identityId) {
        this.request.getRequestContext().getIdentity().setCognitoAuthenticationType("POOL");
        this.request.getRequestContext().getIdentity().setCognitoIdentityId(identityId);
        if (this.request.getRequestContext().getAuthorizer() == null) {
            this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
        }
        this.request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
        this.request.getRequestContext().getAuthorizer().getClaims().setSubject(identityId);

        return this;
    }

    public AwsProxyRequestBuilder claim(String claim, String value) {
        this.request.getRequestContext().getAuthorizer().getClaims().setClaim(claim, value);

        return this;
    }


    public AwsProxyRequestBuilder cognitoIdentity(String identityId, String identityPoolId) {
        this.request.getRequestContext().getIdentity().setCognitoAuthenticationType("IDENTITY");
        this.request.getRequestContext().getIdentity().setCognitoIdentityId(identityId);
        this.request.getRequestContext().getIdentity().setCognitoIdentityPoolId(identityPoolId);
        return this;
    }


    public AwsProxyRequestBuilder cookie(String name, String value) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }

        String cookies = request.getMultiValueHeaders().getFirst(HttpHeaders.COOKIE);
        if (cookies == null) {
            cookies = "";
        }

        cookies += (cookies.equals("")?"":"; ") + name + "=" + value;
        request.getMultiValueHeaders().putSingle(HttpHeaders.COOKIE, cookies);
        return this;
    }

    public AwsProxyRequestBuilder scheme(String scheme) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }

        request.getMultiValueHeaders().putSingle("CloudFront-Forwarded-Proto", scheme);
        return this;
    }

    public AwsProxyRequestBuilder serverName(String serverName) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }

        request.getMultiValueHeaders().putSingle("Host", serverName);
        return this;
    }

    public AwsProxyRequestBuilder userAgent(String agent) {
        if (request.getRequestContext() == null) {
            request.setRequestContext(new AwsProxyRequestContext());
        }
        if (request.getRequestContext().getIdentity() == null) {
            request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        }

        request.getRequestContext().getIdentity().setUserAgent(agent);
        return this;
    }

    public AwsProxyRequestBuilder referer(String referer) {
        if (request.getRequestContext() == null) {
            request.setRequestContext(new AwsProxyRequestContext());
        }
        if (request.getRequestContext().getIdentity() == null) {
            request.getRequestContext().setIdentity(new ApiGatewayRequestIdentity());
        }

        request.getRequestContext().getIdentity().setCaller(referer);
        return this;
    }


    public AwsProxyRequestBuilder basicAuth(String username, String password) {
        // we remove the existing authorization strategy
        request.getMultiValueHeaders().remove(HttpHeaders.AUTHORIZATION);
        String authHeader = "Basic " + Base64.getMimeEncoder().encodeToString((username + ":" + password).getBytes(Charset.defaultCharset()));
        request.getMultiValueHeaders().add(HttpHeaders.AUTHORIZATION, authHeader);
        return this;
    }

    public AwsProxyRequestBuilder fromJsonString(String jsonContent)
            throws IOException {
        request = LambdaContainerHandler.getObjectMapper().readValue(jsonContent, AwsProxyRequest.class);
        return this;
    }

    @SuppressFBWarnings("PATH_TRAVERSAL_IN")
    public AwsProxyRequestBuilder fromJsonPath(String filePath)
            throws IOException {
        request = LambdaContainerHandler.getObjectMapper().readValue(new File(filePath), AwsProxyRequest.class);
        return this;
    }

    public AwsProxyRequest build() {
        return this.request;
    }

    public InputStream buildStream() {
        try {
            String requestJson = LambdaContainerHandler.getObjectMapper().writeValueAsString(request);
            return new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public InputStream toHttpApiV2RequestStream() {
        HttpApiV2ProxyRequest req = toHttpApiV2Request();
        try {
            String requestJson = LambdaContainerHandler.getObjectMapper().writeValueAsString(req);
            return new ByteArrayInputStream(requestJson.getBytes(StandardCharsets.UTF_8));
        } catch (JsonProcessingException e) {
            return null;
        }
    }

    public HttpApiV2ProxyRequest toHttpApiV2Request() {
        HttpApiV2ProxyRequest req = new HttpApiV2ProxyRequest();
        req.setRawPath(request.getPath());
        req.setBase64Encoded(request.isBase64Encoded());
        req.setBody(request.getBody());
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().containsKey(HttpHeaders.COOKIE)) {
            req.setCookies(Arrays.asList(request.getMultiValueHeaders().getFirst(HttpHeaders.COOKIE).split(";")));
        }
        req.setHeaders(new TreeMap<>(String.CASE_INSENSITIVE_ORDER));
        if (request.getMultiValueHeaders() != null) {
            request.getMultiValueHeaders().forEach((key, value) -> {
                if (!HttpHeaders.COOKIE.equals(key)) {
                    req.getHeaders().put(key, value.get(0));
                }
            });
        }
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null) {
            if (request.getRequestContext().getIdentity().getCaller() != null) {
                req.getHeaders().put("Referer", request.getRequestContext().getIdentity().getCaller());
            }
            if (request.getRequestContext().getIdentity().getUserAgent() != null) {
                req.getHeaders().put(HttpHeaders.USER_AGENT, request.getRequestContext().getIdentity().getUserAgent());
            }

        }
        if (request.getMultiValueQueryStringParameters() != null) {
            StringBuilder rawQueryString = new StringBuilder();
            request.getMultiValueQueryStringParameters().forEach((k, v) -> {
                for (String s : v) {
                    rawQueryString.append("&");
                    try {
                        // same terrible hack as the alb() method. Because our spring tests use commas as control characters
                        // we do not encode it
                        rawQueryString.append(URLEncoder.encode(k, "UTF-8").replaceAll("%2C", ","));
                        rawQueryString.append("=");
                        rawQueryString.append(URLEncoder.encode(s, "UTF-8").replaceAll("%2C", ","));
                    } catch (UnsupportedEncodingException e) {
                        throw new RuntimeException(e);
                    }
                }
            });
            String qs = rawQueryString.toString();
            if (qs.length() > 1) {
                req.setRawQueryString(qs.substring(1));
            }
        }
        req.setRouteKey("$default");
        req.setVersion("2.0");
        req.setStageVariables(request.getStageVariables());

        HttpApiV2ProxyRequestContext ctx = new HttpApiV2ProxyRequestContext();
        HttpApiV2HttpContext httpCtx = new HttpApiV2HttpContext();
        httpCtx.setMethod(request.getHttpMethod());
        httpCtx.setPath(request.getPath());
        httpCtx.setProtocol("HTTP/1.1");
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null && request.getRequestContext().getIdentity().getSourceIp() != null) {
            httpCtx.setSourceIp(request.getRequestContext().getIdentity().getSourceIp());
        } else {
            httpCtx.setSourceIp("127.0.0.1");
        }
        if (request.getRequestContext() != null && request.getRequestContext().getIdentity() != null && request.getRequestContext().getIdentity().getUserAgent() != null) {
            httpCtx.setUserAgent(request.getRequestContext().getIdentity().getUserAgent());
        }
        ctx.setHttp(httpCtx);
        if (request.getRequestContext() != null) {
            ctx.setAccountId(request.getRequestContext().getAccountId());
            ctx.setApiId(request.getRequestContext().getApiId());
            ctx.setDomainName(request.getRequestContext().getApiId() + ".execute-api.us-east-1.apigateway.com");
            ctx.setDomainPrefix(request.getRequestContext().getApiId());
            ctx.setRequestId(request.getRequestContext().getRequestId());
            ctx.setRouteKey("$default");
            ctx.setStage(request.getRequestContext().getStage());
            ctx.setTimeEpoch(request.getRequestContext().getRequestTimeEpoch());
            ctx.setTime(request.getRequestContext().getRequestTime());

            if (request.getRequestContext().getAuthorizer() != null) {
                HttpApiV2AuthorizerMap auth = new HttpApiV2AuthorizerMap();
                HttpApiV2JwtAuthorizer jwt = new HttpApiV2JwtAuthorizer();
                // TODO: Anything we should map here?
                jwt.setClaims(new HashMap<>());
                jwt.setScopes(new ArrayList<>());
                auth.putJwtAuthorizer(jwt);
                ctx.setAuthorizer(auth);
            }
        }
        req.setRequestContext(ctx);

        return req;
    }
}
