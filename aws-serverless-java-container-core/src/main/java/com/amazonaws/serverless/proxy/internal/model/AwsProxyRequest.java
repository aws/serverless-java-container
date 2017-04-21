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
package com.amazonaws.serverless.proxy.internal.model;

import com.fasterxml.jackson.annotation.JsonIgnore;

import java.util.Map;

/**
 * Default implementation of the request object from an API Gateway AWS_PROXY integration
 */
public class AwsProxyRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String body;
    private String resource;
    private ApiGatewayRequestContext requestContext;
    private Map<String, String> queryStringParameters;
    private Map<String, String> headers;
    private Map<String, String> pathParameters;
    private String httpMethod;
    private Map<String, String> stageVariables;
    private String path;
    private boolean isBase64Encoded;


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    @JsonIgnore
    public String getQueryString() {
        StringBuilder params = new StringBuilder("");

        if (this.getQueryStringParameters() != null && this.getQueryStringParameters().size() > 0) {
            for (String key : this.getQueryStringParameters().keySet()) {
                String separator = params.length() == 0 ? "?" : "&";

                params.append(separator + key + "=" + this.getQueryStringParameters().get(key));
            }
        }

        return params.toString();
    }


    public String getBody() {
        return body;
    }


    public void setBody(String body) {
        this.body = body;
    }


    public String getResource() {
        return resource;
    }


    public void setResource(String resource) {
        this.resource = resource;
    }


    public ApiGatewayRequestContext getRequestContext() {
        return requestContext;
    }


    public void setRequestContext(ApiGatewayRequestContext requestContext) {
        this.requestContext = requestContext;
    }


    public Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }


    public void setQueryStringParameters(Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }


    public Map<String, String> getHeaders() {
        return headers;
    }


    public void setHeaders(Map<String, String> headers) {
        this.headers = headers;
    }


    public Map<String, String> getPathParameters() {
        return pathParameters;
    }


    public void setPathParameters(Map<String, String> pathParameters) {
        this.pathParameters = pathParameters;
    }


    public String getHttpMethod() {
        return httpMethod;
    }


    public void setHttpMethod(String httpMethod) {
        this.httpMethod = httpMethod;
    }


    public Map<String, String> getStageVariables() {
        return stageVariables;
    }


    public void setStageVariables(Map<String, String> stageVariables) {
        this.stageVariables = stageVariables;
    }


    public String getPath() {
        return path;
    }


    public void setPath(String path) {
        this.path = path;
    }


    public boolean isBase64Encoded() {
        return isBase64Encoded;
    }


    public void setIsBase64Encoded(boolean base64Encoded) {
        isBase64Encoded = base64Encoded;
    }
}
