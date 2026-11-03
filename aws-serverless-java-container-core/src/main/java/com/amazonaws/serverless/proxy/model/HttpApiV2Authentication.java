/*
 * Copyright 2026 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.amazonaws.serverless.proxy.model;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

/**
 * Authentication metadata included in an API Gateway HTTP API v2 request context.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class HttpApiV2Authentication {
    private ApiGatewayClientCertificate clientCert;

    public ApiGatewayClientCertificate getClientCert() {
        return clientCert;
    }

    public void setClientCert(ApiGatewayClientCertificate clientCert) {
        this.clientCert = clientCert;
    }
}
