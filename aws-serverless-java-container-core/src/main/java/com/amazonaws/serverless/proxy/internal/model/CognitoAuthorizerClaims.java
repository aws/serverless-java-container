/*
 * Copyright 2017 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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


import com.fasterxml.jackson.annotation.JsonProperty;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;


/**
 * This object represents the claims property in the authorizer context of a request. The claims object is normally populated
 * by a Cognito User Pool authorizer and contains the following fields:
 * <pre>
 * "claims": {
 *     "sub": "42df3b02-29f1-4779-a3e5-eff92ff280b2",
 *     "aud": "2k3no2j1rjjbqaskc4bk0ub29b",
 *     "email_verified": "true",
 *     "token_use": "id",
 *     "auth_time": "1492467169",
 *     "iss": "https://cognito-idp.us-east-2.amazonaws.com/us-east-2_Adx5ZHePg",
 *     "cognito:username": "sapessi",
 *     "exp": "Mon Apr 17 23:12:49 UTC 2017",
 *     "iat": "Mon Apr 17 22:12:49 UTC 2017",
 *     "email": "bulianis@amazon.com"
 * }
 * </pre>
 */
public class CognitoAuthorizerClaims {

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    // Mon Apr 17 23:12:49 UTC 2017
    static final DateTimeFormatter TOKEN_DATE_FORMATTER = DateTimeFormatter.ofPattern("EEE MMM dd HH:mm:ss zzz yyyy");


    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    @JsonProperty(value = "sub")
    private String subject;
    @JsonProperty(value = "aud")
    private String audience;
    @JsonProperty(value = "iss")
    private String issuer;
    @JsonProperty(value = "token_use")
    private String tokenUse;
    @JsonProperty(value = "cognito:username")
    private String username;
    private String email;
    @JsonProperty(value = "email_verified")
    private boolean emailVerified;
    @JsonProperty(value = "auth_time")
    private Long authTime;
    private String exp;
    private String iat;


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getSubject() { return this.subject; }


    public void setSubject(String subject) {
        this.subject = subject;
    }


    public String getAudience() {
        return audience;
    }


    public void setAudience(String audience) {
        this.audience = audience;
    }


    public String getIssuer() {
        return issuer;
    }


    public void setIssuer(String issuer) {
        this.issuer = issuer;
    }


    public String getTokenUse() {
        return tokenUse;
    }


    public void setTokenUse(String tokenUse) {
        this.tokenUse = tokenUse;
    }


    public String getUsername() {
        return username;
    }


    public void setUsername(String username) {
        this.username = username;
    }


    public String getEmail() {
        return email;
    }


    public void setEmail(String email) {
        this.email = email;
    }


    public boolean isEmailVerified() {
        return emailVerified;
    }


    public void setEmailVerified(boolean emailVerified) {
        this.emailVerified = emailVerified;
    }


    public Long getAuthTime() {
        return authTime;
    }


    public void setAuthTime(Long authTime) {
        this.authTime = authTime;
    }


    public String getExp() {
        return exp;
    }


    public void setExp(String expiration) {
        this.exp = expiration;
    }


    /**
     * Returns the expiration time for the token as a <code>ZonedDateTime</code> from the <code>exp</code> property
     * of the token.
     * @return The parsed expiration time for the token.
     */
    public ZonedDateTime getExpirationTime() {
        return ZonedDateTime.from(TOKEN_DATE_FORMATTER.parse(getExp()));
    }


    public String getIat() {
        return iat;
    }


    public void setIat(String issuedAt) {
        this.iat = issuedAt;
    }


    /**
     * Returns the parsed issued time for the token as a <code>ZonedDateTime</code> object. This is taken from the <code>iat</code>
     * property of the token.
     * @return The parsed issue time of the token
     */
    public ZonedDateTime getIssueTime() {
        return ZonedDateTime.from((TOKEN_DATE_FORMATTER.parse(getIat())));
    }
}
