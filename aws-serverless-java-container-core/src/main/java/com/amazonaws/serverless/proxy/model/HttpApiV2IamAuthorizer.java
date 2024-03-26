package com.amazonaws.serverless.proxy.model;

public class HttpApiV2IamAuthorizer {
    public String accessKey;
    public String accountId;
    public String callerId;
    public String cognitoIdentity;
    public String principalOrgId;
    public String userArn;
    public String userId;

    public String getAccessKey() {
        return accessKey;
    }

    public String getAccountId() {
        return accountId;
    }

    public String getCallerId() {
        return callerId;
    }

    public String getCognitoIdentity() {
        return cognitoIdentity;
    }

    public String getPrincipalOrgId() {
        return principalOrgId;
    }

    public String getUserArn() {
        return userArn;
    }

    public String getUserId() {
        return userId;
    }

    public void setAccessKey(String accessKey) {
        this.accessKey = accessKey;
    }

    public void setAccountId(String accountId) {
        this.accountId = accountId;
    }

    public void setCallerId(String callerId) {
        this.callerId = callerId;
    }

    public void setCognitoIdentity(String cognitoIdentity) {
        this.cognitoIdentity = cognitoIdentity;
    }

    public void setPrincipalOrgId(String principalOrgId) {
        this.principalOrgId = principalOrgId;
    }

    public void setUserArn(String userArn) {
        this.userArn = userArn;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

}