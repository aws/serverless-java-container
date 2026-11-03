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
 * Client certificate metadata included in API Gateway proxy request events when mutual TLS is enabled.
 */
@JsonIgnoreProperties(ignoreUnknown = true)
public class ApiGatewayClientCertificate {
    private String clientCertPem;
    private String subjectDN;
    private String issuerDN;
    private String serialNumber;
    private Validity validity;

    public String getClientCertPem() {
        return clientCertPem;
    }

    public void setClientCertPem(String clientCertPem) {
        this.clientCertPem = clientCertPem;
    }

    public String getSubjectDN() {
        return subjectDN;
    }

    public void setSubjectDN(String subjectDN) {
        this.subjectDN = subjectDN;
    }

    public String getIssuerDN() {
        return issuerDN;
    }

    public void setIssuerDN(String issuerDN) {
        this.issuerDN = issuerDN;
    }

    public String getSerialNumber() {
        return serialNumber;
    }

    public void setSerialNumber(String serialNumber) {
        this.serialNumber = serialNumber;
    }

    public Validity getValidity() {
        return validity;
    }

    public void setValidity(Validity validity) {
        this.validity = validity;
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public static class Validity {
        private String notBefore;
        private String notAfter;

        public String getNotBefore() {
            return notBefore;
        }

        public void setNotBefore(String notBefore) {
            this.notBefore = notBefore;
        }

        public String getNotAfter() {
            return notAfter;
        }

        public void setNotAfter(String notAfter) {
            this.notAfter = notAfter;
        }
    }
}
