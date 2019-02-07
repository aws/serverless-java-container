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
import com.amazonaws.serverless.proxy.model.AlbContext;
import com.amazonaws.serverless.proxy.model.ApiGatewayAuthorizerContext;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.CognitoAuthorizerClaims;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.apache.commons.io.IOUtils;
import org.apache.http.HttpEntity;
import org.apache.http.entity.mime.MultipartEntityBuilder;
import org.apache.http.entity.mime.content.ByteArrayBody;

import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;

import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.Base64;
import java.util.UUID;


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


    public AwsProxyRequestBuilder(String path, String httpMethod) {
        this.request = new AwsProxyRequest();
        this.request.setMultiValueHeaders(new Headers()); // avoid NPE
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
        this.request.setRequestContext(new AwsProxyRequestContext());
        this.request.getRequestContext().setElb(new AlbContext());
        this.request.getRequestContext().getElb().setTargetGroupArn(
                "arn:aws:elasticloadbalancing:us-east-1:123456789012:targetgroup/lambda-target/d6190d154bc908a5"
        );
        return this;
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
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        request.getMultiValueHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.APPLICATION_FORM_URLENCODED);
        String body = request.getBody();
        if (body == null) {
            body = "";
        }
        body += (body.equals("")?"":"&") + key + "=" + value;
        request.setBody(body);
        return this;
    }

    public AwsProxyRequestBuilder formFilePart(String fieldName, String fileName, byte[] content) throws IOException {
        if (multipartBuilder == null) {
            multipartBuilder = MultipartEntityBuilder.create();
        }
        multipartBuilder.addPart(fieldName, new ByteArrayBody(content, fileName));
        HttpEntity bodyEntity = multipartBuilder.build();
        InputStream bodyStream = bodyEntity.getContent();
        byte[] buffer = new byte[bodyStream.available()];
        IOUtils.readFully(bodyStream, buffer);
        request.setBody("\n\n" + new String(buffer, Charset.defaultCharset()));
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_TYPE, bodyEntity.getContentType().getValue());
        if (bodyEntity.getContentEncoding() != null) {
            request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, bodyEntity.getContentEncoding().getValue());
        }
        request.getMultiValueHeaders().putSingle(HttpHeaders.CONTENT_LENGTH, bodyEntity.getContentLength() + "");
        return this;
    }

    public AwsProxyRequestBuilder formFieldPart(String fieldName, String fieldValue) {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        request.getMultiValueHeaders().add(HttpHeaders.CONTENT_TYPE, MediaType.MULTIPART_FORM_DATA);
        if (multipartBuilder == null) {
            multipartBuilder = MultipartEntityBuilder.create();
        }
        // TODO: implement
        return this;
    }


    public AwsProxyRequestBuilder header(String key, String value) {
        if (this.request.getMultiValueHeaders() == null) {
            this.request.setMultiValueHeaders(new Headers());
        }

        this.request.getMultiValueHeaders().add(key, value);
        return this;
    }


    public AwsProxyRequestBuilder queryString(String key, String value) {
        if (this.request.getMultiValueQueryStringParameters() == null) {
            this.request.setMultiValueQueryStringParameters(new MultiValuedTreeMap<>());
        }

        if (request.getRequestSource() == AwsProxyRequest.RequestSource.API_GATEWAY) {
            this.request.getMultiValueQueryStringParameters().add(key, value);
        }
        // ALB does not decode parameters automatically like API Gateway.
        if (request.getRequestSource() == AwsProxyRequest.RequestSource.ALB) {
            try {
                //if (URLDecoder.decode(value, ContainerConfig.DEFAULT_CONTENT_CHARSET).equals(value)) {
                // TODO: Assume we are always given an unencoded value, smarter check here to encode
                // only if necessary
                this.request.getMultiValueQueryStringParameters().add(
                        key,
                        URLEncoder.encode(value, ContainerConfig.DEFAULT_CONTENT_CHARSET)
                );
                //}
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                throw new RuntimeException(e);
            }

        }
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
        if (request.getMultiValueHeaders() != null && request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE).equals(MediaType.APPLICATION_JSON)) {
            try {
                return body(LambdaContainerHandler.getObjectMapper().writeValueAsString(body));
            } catch (JsonProcessingException e) {
                throw new UnsupportedOperationException("Could not serialize object: " + e.getMessage());
            }
        } else {
            throw new UnsupportedOperationException("Unsupported content type in request");
        }
    }

    public AwsProxyRequestBuilder binaryBody(InputStream is)
            throws IOException {
        this.request.setIsBase64Encoded(true);
        return body(Base64.getMimeEncoder().encodeToString(IOUtils.toByteArray(is)));
    }


    public AwsProxyRequestBuilder authorizerPrincipal(String principal) {
        if (this.request.getRequestSource() == AwsProxyRequest.RequestSource.API_GATEWAY) {
            if (this.request.getRequestContext().getAuthorizer() == null) {
                this.request.getRequestContext().setAuthorizer(new ApiGatewayAuthorizerContext());
            }
            this.request.getRequestContext().getAuthorizer().setPrincipalId(principal);
            if (this.request.getRequestContext().getAuthorizer().getClaims() == null) {
                this.request.getRequestContext().getAuthorizer().setClaims(new CognitoAuthorizerClaims());
            }
            this.request.getRequestContext().getAuthorizer().getClaims().setSubject(principal);
        }
        if (this.request.getRequestSource() == AwsProxyRequest.RequestSource.ALB) {
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
}
