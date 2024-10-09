package com.amazonaws.serverless.proxy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import org.jetbrains.annotations.Nullable;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Map;

@JsonIgnoreProperties(ignoreUnknown = true)
public class VPCLatticeV2RequestEvent {
    private String version;
    private String path;
    private String method;
    private Headers headers;
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

    public RequestContext getRequestContext() {
        return requestContext;
    }

    public String getVersion() {
        return version;
    }

    public String getPath() {
        return path;
    }

    public String getMethod() {
        return method;
    }

    public String getBody() {
        return body;
    }

    public Headers getHeaders() {
        return headers;
    }

    public @Nullable Boolean getIsBase64Encoded() {
        return isBase64Encoded;
    }

    public @Nullable Map<String, String> getQueryStringParameters() {
        return queryStringParameters;
    }

    public void setPath(String s) {
        this.path = s;
    }

    public void setVersion(String version) {
        this.version = version;
    }

    public void setMethod(String method) {
        this.method = method;
    }

    public void setHeaders(Headers headers) {
        this.headers = headers;
    }

    public void setQueryStringParameters(@Nullable Map<String, String> queryStringParameters) {
        this.queryStringParameters = queryStringParameters;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public void setBase64Encoded(Boolean base64Encoded) {
        isBase64Encoded = base64Encoded;
    }

    public void setRequestContext(RequestContext requestContext) {
        this.requestContext = requestContext;
    }

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

        public Identity getIdentity() {
            return identity;
        }

        public String getTimeEpoch() {
            return timeEpoch;
        }

        public String getRegion() {
            return region;
        }

        public String getServiceNetworkArn() {
            return serviceNetworkArn;
        }

        public String getServiceArn() {
            return serviceArn;
        }

        public String getTargetGroupArn() {
            return targetGroupArn;
        }

        public void setServiceNetworkArn(String serviceNetworkArn) {
            this.serviceNetworkArn = serviceNetworkArn;
        }

        public void setServiceArn(String serviceArn) {
            this.serviceArn = serviceArn;
        }

        public void setTargetGroupArn(String targetGroupArn) {
            this.targetGroupArn = targetGroupArn;
        }

        public void setIdentity(Identity identity) {
            this.identity = identity;
        }

        public void setRegion(String region) {
            this.region = region;
        }

        public void setTimeEpoch(String timeEpoch) {
            this.timeEpoch = timeEpoch;
        }
    }

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

        public String getPrincipal() {
            return principal;
        }

        public String getType() {
            return type;
        }

        public String getSessionName() {
            return sessionName;
        }

        public String getX509IssuerOu() {
            return x509IssuerOu;
        }

        public String getX509SanDns() {
            return x509SanDns;
        }

        public String getX509SanNameCn() {
            return x509SanNameCn;
        }

        public String getX509SanUri() {
            return x509SanUri;
        }

        public String getX509SubjectCn() {
            return x509SubjectCn;
        }

        public String getSourceVpcArn() {
            return sourceVpcArn;
        }

        public void setSourceVpcArn(String sourceVpcArn) {
            this.sourceVpcArn = sourceVpcArn;
        }

        public void setType(String type) {
            this.type = type;
        }

        public void setPrincipal(String principal) {
            this.principal = principal;
        }

        public void setSessionName(String sessionName) {
            this.sessionName = sessionName;
        }

        public void setX509SanDns(String x509SanDns) {
            this.x509SanDns = x509SanDns;
        }

        public void setX509SanNameCn(String x509SanNameCn) {
            this.x509SanNameCn = x509SanNameCn;
        }

        public void setX509SubjectCn(String x509SubjectCn) {
            this.x509SubjectCn = x509SubjectCn;
        }

        public void setX509IssuerOu(String x509IssuerOu) {
            this.x509IssuerOu = x509IssuerOu;
        }

        public void setX509SanUri(String x509SanUri) {
            this.x509SanUri = x509SanUri;
        }
    }
}
