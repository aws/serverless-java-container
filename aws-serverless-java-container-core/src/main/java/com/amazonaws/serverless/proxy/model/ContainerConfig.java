/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
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

import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;

/**
 * Configuration parameters for the framework
 */
public class ContainerConfig {
    public static final String DEFAULT_URI_ENCODING = "UTF-8";
    public static final String DEFAULT_CONTENT_CHARSET = "ISO-8859-1";
    private static final List<String> DEFAULT_FILE_PATHS = new ArrayList<String>() {{ add("/tmp"); add("/var/task"); }};
    private static final int MAX_INIT_TIMEOUT_MS = 20_000;

    public static ContainerConfig defaultConfig() {
        ContainerConfig configuration = new ContainerConfig();
        configuration.setStripBasePath(false);
        configuration.setUriEncoding(DEFAULT_URI_ENCODING);
        configuration.setConsolidateSetCookieHeaders(false);
        configuration.setUseStageAsServletContext(false);
        configuration.setValidFilePaths(DEFAULT_FILE_PATHS);
        configuration.setQueryStringCaseSensitive(false);
        configuration.addBinaryContentTypes("application/octet-stream", "image/jpeg", "image/png", "image/gif");
        configuration.setDefaultContentCharset(DEFAULT_CONTENT_CHARSET);
        configuration.setInitializationTimeout(MAX_INIT_TIMEOUT_MS);
        configuration.setDisableExceptionMapper(false);

        return configuration;
    }

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String serviceBasePath;
    private boolean stripBasePath;
    private String uriEncoding;
    private String defaultContentCharset;
    private boolean consolidateSetCookieHeaders;
    private boolean useStageAsServletContext;
    private List<String> validFilePaths;
    private List<String> customDomainNames;
    private boolean queryStringCaseSensitive;
    private final HashSet<String> binaryContentTypes;
    private int initializationTimeout;
    private boolean disableExceptionMapper;

    public ContainerConfig() {
        validFilePaths = new ArrayList<>();
        customDomainNames = new ArrayList<>();
        binaryContentTypes = new HashSet<>();
    }


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------


    /**
     * Returns the base path configured in the container. This configuration variable is used in conjuction with {@link #setStripBasePath(boolean)} to route
     * the request. When requesting the context path from an HttpServletRequest: {@link AwsProxyHttpServletRequest#getContextPath()} this base path is added
     * to the context even though it was initially stripped for the purpose of routing the request. We decided to add it to the context to address GitHub issue
     * #84 and allow framework's link builders to it.
     *
     * @return The base path configured for the container
     */
    public String getServiceBasePath() {
        return serviceBasePath;
    }


    /**
     * Configures a base path  that can be stripped from the request path before passing it to the framework-specific implementation. This can be used to
     * remove API Gateway's base path mappings from the request.
     * @param serviceBasePath The base path mapping to be removed.
     */
    public void setServiceBasePath(String serviceBasePath) {
        if (serviceBasePath == null) {
            this.serviceBasePath = null;
            return;
        }
        // clean up base path before setting it, we want a "/" at the beginning but not at the end.
        String finalBasePath = serviceBasePath;
        if (!finalBasePath.startsWith("/")) {
            finalBasePath = "/" + serviceBasePath;
        }
        if (finalBasePath.endsWith("/")) {
            finalBasePath = finalBasePath.substring(0, finalBasePath.length() - 1);
        }
        this.serviceBasePath = finalBasePath;
    }


    public boolean isStripBasePath() {
        return stripBasePath;
    }


    /**
     * Whether this framework should strip the base path mapping specified with the {@link #setServiceBasePath(String)} method from a request before
     * passing it to the framework-specific implementations
     * @param stripBasePath
     */
    public void setStripBasePath(boolean stripBasePath) {
        this.stripBasePath = stripBasePath;
    }


    public String getUriEncoding() {
        return uriEncoding;
    }


    /**
     * Sets the charset used to URLEncode and Decode request paths.
     * @param uriEncoding The charset. By default this is set to UTF-8
     */
    public void setUriEncoding(String uriEncoding) {
        this.uriEncoding = uriEncoding;
    }


    public boolean isConsolidateSetCookieHeaders() {
        return consolidateSetCookieHeaders;
    }


    /**
     * Tells the library to consolidate multiple Set-Cookie headers into a single Set-Cookie header with multiple, comma-separated values. This is allowed
     * by the RFC 2109 (https://tools.ietf.org/html/rfc2109). However, since not all clients support this, we consider it optional. When this value is set
     * to true the framework will consolidate all Set-Cookie headers into a single header, when it's set to false, the framework will only return the first
     * Set-Cookie header specified in a response.
     *
     * Because API Gateway needs header keys to be unique, we give an option to configure this.
     * @param consolidateSetCookieHeaders Whether to consolidate the cookie headers or not.
     */
    public void setConsolidateSetCookieHeaders(boolean consolidateSetCookieHeaders) {
        this.consolidateSetCookieHeaders = consolidateSetCookieHeaders;
    }


    /**
     * Tells whether the stage name passed in the request should be added to the context path: {@link AwsProxyHttpServletRequest#getContextPath()}.
     * @return true if the stage will be included in the context path, false otherwise.
     */
    public boolean isUseStageAsServletContext() {
        return useStageAsServletContext;
    }


    /**
     * Sets whether the API Gateway stage name should be included in the servlet context path.
     * @param useStageAsServletContext true if you want the stage to appear as the root of the context path, false otherwise.
     */
    public void setUseStageAsServletContext(boolean useStageAsServletContext) {
        this.useStageAsServletContext = useStageAsServletContext;
    }


    /**
     * Returns the list of file paths that the servlet accepts read/write requests to
     * @return A List of file paths. By default this is set to /tmp and /var/task
     */
    public List<String> getValidFilePaths() {
        return validFilePaths;
    }


    /**
     * Sets a list of valid file paths for the servlet to read/write from.
     * @param validFilePaths A populated list of base paths
     */
    public void setValidFilePaths(List<String> validFilePaths) {
        this.validFilePaths = validFilePaths;
    }


    /**
     * Adds a new base path to the list of allowed paths.
     * @param filePath The base path
     */
    public void addValidFilePath(String filePath) {
        validFilePaths.add(filePath);
    }


    /**
     * Adds a new custom domain name to the list of allowed domains
     * @param name The new custom domain name, excluding the scheme ("https") and port
     */
    public void addCustomDomain(String name) {
        customDomainNames.add(name);
    }


    /**
     * Returns the list of custom domain names enabled for the application
     * @return The configured custom domain names
     */
    public List<String> getCustomDomainNames() {
        return customDomainNames;
    }


    /**
     * Enables localhost custom domain name for testing. This setting should be used only in local
     * with SAM local
     */
    public void enableLocalhost() {
        customDomainNames.add("localhost");
    }


    /**
     * Whether query string parameters in the request should be case sensitive or not. By default
     * this is set to <code>false</code> for backward compatibility.
     * @return <code>true</code> if the parameter matching algorithm is case sensitive
     */
    public boolean isQueryStringCaseSensitive() {
        return queryStringCaseSensitive;
    }


    /**
     * Sets whether query string parameter names should be treated as case sensitive. The default
     * value of this option is <code>false</code> for backward compatibility.
     * @param queryStringCaseSensitive Tells the framework to treat query string parameter names as case sensitive
     */
    public void setQueryStringCaseSensitive(boolean queryStringCaseSensitive) {
        this.queryStringCaseSensitive = queryStringCaseSensitive;
    }

    /**
     * Configure specified content type(s) as binary
     * @param contentTypes list of exact content types that will be considered as binary
     */
    public void addBinaryContentTypes(String... contentTypes) {
        if(contentTypes != null) {
            binaryContentTypes.addAll(Arrays.asList(contentTypes));
        }
    }

    /**
     * Determine if specified content type has been configured as binary
     * @param contentType content type to query
     * @return
     */
    public boolean isBinaryContentType(String contentType) {
        return contentType != null && binaryContentTypes.contains(contentType.trim());
    }


    /**
     * Returns the name of the default charset appended to the <code>Content-Type</code> header if no charset is specified by the request. The
     * default value of this is <code>ISO-8859-1</code>.
     * @return The name of the default charset for the Content-Type header
     */
    public String getDefaultContentCharset() {
        return defaultContentCharset;
    }


    /**
     * Sets the default charset value for the <code>Content-Type</code> header if no charset is specified with a request. The default value of this
     * is <code>ISO-8859-1</code>. If a request specifies a <code>Content-Type</code> header without a <code>charset</code> property, the value of
     * this field is automatically appended to the header.
     * @param defaultContentCharset The name of the charset for the content type header.
     */
    public void setDefaultContentCharset(String defaultContentCharset) {
        this.defaultContentCharset = defaultContentCharset;
    }

    /**
     * Returns the maximum amount of time (in milliseconds) set for the initialization time. See documentation on the
     * {@link #setInitializationTimeout(int)} for additional details.
     * @return The max time allocated for initialization
     */
    public int getInitializationTimeout() {
        return initializationTimeout;
    }

    /**
     * Sets the initialization timeout. When using an async {@link com.amazonaws.serverless.proxy.InitializationWrapper}
     * the underlying framework is initialized in a separate thread. Serverless Java Container will wait for the maximum
     * time available during AWS Lambda's init step (~10 seconds) and then return control to the main thread. In the meanwhile,
     * the initialization process of the underlying framework can continue in a separate thread. AWS Lambda will then call
     * the handler class to handle an event. This timeout is the maximum amount of time Serverless Java Container framework
     * will wait for the underlying framework to initialize before returning an error. By default, this is set to 10 seconds.
     * @param initializationTimeout The maximum amount of time to wait for the underlying framework initialization after
     *                              an event is received in milliseconds.
     */
    public void setInitializationTimeout(int initializationTimeout) {
        this.initializationTimeout = initializationTimeout;
    }

    /**
     * Whether the framework will run exception thrown by the application through the implementation of
     * {@link com.amazonaws.serverless.proxy.ExceptionHandler}. When this parameter is set to false the Lambda
     * container handler object lets the Exception propagate upwards to the Lambda handler class.
     * @return <code>true</code> if exception mapping is disabled, <code>false</code> otherwise.
     */
    public boolean isDisableExceptionMapper() {
        return disableExceptionMapper;
    }

    /**
     * This configuration parameter tells the container whether it should skip exception mapping and simply let any
     * Exception thrown by the underlying application bubble up to the Lambda handler class.
     * @param disable Set this value to <code>true</code> to disable exception mapping, <code>false</code> otherwise.
     */
    public void setDisableExceptionMapper(boolean disable) {
        this.disableExceptionMapper = disable;
    }
}
