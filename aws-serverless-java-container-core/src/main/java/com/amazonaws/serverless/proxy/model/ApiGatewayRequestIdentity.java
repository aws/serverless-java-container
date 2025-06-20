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
package com.amazonaws.serverless.proxy.model;


import com.amazonaws.serverless.proxy.RequestReader;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;


/**
 * Identity model for the API Gateway request context. This is used in the default AwsProxyRequest object. Contains
 * all of the properties declared in the $context.identity API Gateway object so could be re-used for other implementations
 *
 * @see AwsProxyRequest
 * @see RequestReader
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiGatewayRequestIdentity {

    /**
     * Creates an ApiGatewayRequestIdentity instance with default values.
     * 
     * @return A pre-configured ApiGatewayRequestIdentity instance
     */
    public static ApiGatewayRequestIdentity getApiGatewayRequestIdentity() {
        ApiGatewayRequestIdentity identity = new ApiGatewayRequestIdentity();
        // Set default values for all fields
        identity.setApiKey("");
        identity.setApiKeyId("");
        identity.setUserArn("");
        identity.setCognitoAuthenticationType("");
        identity.setCaller("");
        identity.setUserAgent("");
        identity.setUser("");
        identity.setCognitoIdentityPoolId("");
        identity.setCognitoIdentityId("");
        identity.setCognitoAuthenticationProvider("");
        identity.setSourceIp("127.0.0.1"); // Reasonable default
        identity.setAccountId("");
        identity.setAccessKey("");
        return identity;
    }

    /**
     * Creates an ApiGatewayRequestIdentity instance with the specified source IP.
     * 
     * @param sourceIp the source IP to set
     * @return A pre-configured ApiGatewayRequestIdentity instance
     */
    public static ApiGatewayRequestIdentity getApiGatewayRequestIdentity(String sourceIp) {
        ApiGatewayRequestIdentity identity = getApiGatewayRequestIdentity();
        identity.setSourceIp(sourceIp);
        return identity;
    }

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String apiKey;
    private String apiKeyId;
    private String userArn;
    private String cognitoAuthenticationType;
    private String caller;
    private String userAgent;
    private String user;
    private String cognitoIdentityPoolId;
    private String cognitoIdentityId;
    private String cognitoAuthenticationProvider;
    private String sourceIp;
    private String accountId;
    private String accessKey;


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getApiKey() {
        return apiKey;
    }


    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }


    public String getApiKeyId() {
        return apiKeyId;
    }


    public void setApiKeyId(String apiKeyId) {
        this.apiKeyId = apiKeyId;
    }


    public String getUserArn() {
        return userArn;
    }


    public void setUserArn(String userArn) {
        this.userArn = userArn;
    }


    public String getCognitoAuthenticationType() {
        return cognitoAuthenticationType;
    }


    public void setCognitoAuthenticationType(String cognitoAuthenticationType) {
        this.cognitoAuthenticationType = cognitoAuthenticationType;
    }


    public String getCaller() {
        return caller;
    }


    public void setCaller(String caller) {
        this.caller = caller;
    }


    public String getUserAgent() {
        return userAgent;
    }


    public void setUserAgent(String userAgent) {
        this.userAgent = userAgent;
    }


    public String getUser() {
        return user;
    }


    public void setUser(String user) {
        this.user = user;
    }


    public String getCognitoIdentityPoolId() {
        return cognitoIdentityPoolId;
    }


    public void setCognitoIdentityPoolId(String cognitoIdentityPoolId) {
        this.cognitoIdentityPoolId = cognitoIdentityPoolId;
    }


    public String getCognitoIdentityId() {
        return cognitoIdentityId;
    }


    public void setCognitoIdentityId(String cognitoIdentityId) {
        this.cognitoIdentityId = cognitoIdentityId;
    }


    public String getCognitoAuthenticationProvider() {
        return cognitoAuthenticationProvider;
    }


    public void setCognitoAuthenticationProvider(String cognitoAuthenticationProvider) {
        this.cognitoAuthenticationProvider = cognitoAuthenticationProvider;
    }


    public String getSourceIp() {
        return sourceIp;
    }


    public void setSourceIp(String sourceIp) {
        this.sourceIp = sourceIp;
    }


    public String getAccountId() {
        return accountId;
    }


    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }


    public String getAccessKey() {
        return accessKey;
    }


    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }
}
