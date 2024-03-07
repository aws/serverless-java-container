package com.amazonaws.serverless.proxy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Map;
public class VPCLatticeV2RequestEvent {
    private String version;
    private String path;
    private String method;
    private Map<String, List<String>> headers;
    @Nullable
    private Map<String, String> queryStringParameters;
    private RequestContext requestContext;
    private String body;

    /***
     * isBase64Encoded is set if the body is a base64 encoded String.
     */
    @Nullable
    private Boolean isBase64Encoded;

    @JsonIgnore
    public RequestSource getRequestSource() {
        return RequestSource.VPC_LATTICE_V2;
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(setterPrefix = "with")
    public static class RequestContext {
        private String serviceNetworkArn;
        private String serviceArn;
        private String targetGroupArn;
        private Identity identity;
        private String region;
        /**
         * Number of microseconds from the epoch
         */
        private String timeEpoch;

        @JsonIgnore
        public LocalDateTime getLocalDateTime() {
            long epochMicroseconds = Long.parseLong(timeEpoch);
            long epochMilliseconds = epochMicroseconds / 1000;

            return Instant.ofEpochMilli(epochMilliseconds).atZone(ZoneOffset.UTC).toLocalDateTime();
        }
    }

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @Builder(setterPrefix = "with")
    public static class Identity {
        private String sourceVpcArn;
        private String type;
        private String principal;
        private String sessionName;
        private String x509SanDns;
        private String x509SanNameCn;
        private String x509SubjectCn;
        private String x509IssuerOu;
        private String x509SanUri;
    }
}
