package com.amazonaws.serverless.proxy.model;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ApiGatewayClientCertificateTest {
    private static final String CERTIFICATE_JSON = "\"clientCert\": {"
            + "\"clientCertPem\": \"-----BEGIN CERTIFICATE-----\\ncertificate\\n-----END CERTIFICATE-----\","
            + "\"subjectDN\": \"CN=client\","
            + "\"issuerDN\": \"CN=issuer\","
            + "\"serialNumber\": \"01:02:03\","
            + "\"validity\": {\"notBefore\": \"May 28 12:30:02 2019 GMT\","
            + "\"notAfter\": \"Aug  5 09:36:04 2021 GMT\"}"
            + "}";

    @Test
    void deserialize_restApiIdentityClientCertificate() throws Exception {
        String requestJson = "{\"requestContext\": {\"identity\": {" + CERTIFICATE_JSON + "}}}";

        AwsProxyRequest request = new AwsProxyRequestBuilder().fromJsonString(requestJson).build();
        ApiGatewayClientCertificate certificate = request.getRequestContext().getIdentity().getClientCert();

        assertCertificate(certificate);
    }

    @Test
    void deserialize_httpApiV2AuthenticationClientCertificate() throws Exception {
        String requestJson = "{\"requestContext\": {\"authentication\": {" + CERTIFICATE_JSON + "}}}";

        HttpApiV2ProxyRequest request = LambdaContainerHandler.getObjectMapper()
                .readValue(requestJson, HttpApiV2ProxyRequest.class);
        ApiGatewayClientCertificate certificate = request.getRequestContext().getAuthentication().getClientCert();

        assertCertificate(certificate);
    }

    private void assertCertificate(ApiGatewayClientCertificate certificate) {
        assertNotNull(certificate);
        assertEquals("-----BEGIN CERTIFICATE-----\ncertificate\n-----END CERTIFICATE-----", certificate.getClientCertPem());
        assertEquals("CN=client", certificate.getSubjectDN());
        assertEquals("CN=issuer", certificate.getIssuerDN());
        assertEquals("01:02:03", certificate.getSerialNumber());
        assertNotNull(certificate.getValidity());
        assertEquals("May 28 12:30:02 2019 GMT", certificate.getValidity().getNotBefore());
        assertEquals("Aug  5 09:36:04 2021 GMT", certificate.getValidity().getNotAfter());
    }
}
