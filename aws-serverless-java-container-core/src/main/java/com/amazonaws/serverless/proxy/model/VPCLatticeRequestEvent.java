package com.amazonaws.serverless.proxy.model;

import com.fasterxml.jackson.annotation.JsonIgnore;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

public class VPCLatticeRequestEvent {
    private String raw_path;
    private String method;
    private String body;
    private Map<String, List<String>> headers;
    private Map<String, List<String>> query_string_parameters;

    /***
     * isBase64Encoded is set if the body is a base64 encoded String.
     */
    @Nullable
    private Boolean isBase64Encoded;

    @JsonIgnore
    public RequestSource getRequestSource() {
        return RequestSource.VPC_LATTICE_V1;
    }
}
