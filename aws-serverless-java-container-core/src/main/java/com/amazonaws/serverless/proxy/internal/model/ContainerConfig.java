package com.amazonaws.serverless.proxy.internal.model;


/**
 * Configuration paramters used by the <code>RequestReader</code> and <code>ResponseWriter</code> objects.
 */
public class ContainerConfig {
    public static final String DEFAULT_URI_ENCODING = "UTF-8";

    public static ContainerConfig defaultConfig() {
        ContainerConfig configuration = new ContainerConfig();
        configuration.setStripBasePath(false);
        configuration.setUriEncoding(DEFAULT_URI_ENCODING);

        return configuration;
    }

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private String serviceBasePath;
    private boolean stripBasePath;
    private String uriEncoding;


    //-------------------------------------------------------------
    // Methods - Getter/Setter
    //-------------------------------------------------------------

    public String getServiceBasePath() {
        return serviceBasePath;
    }


    public void setServiceBasePath(String serviceBasePath) {
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


    public void setStripBasePath(boolean stripBasePath) {
        this.stripBasePath = stripBasePath;
    }


    public String getUriEncoding() {
        return uriEncoding;
    }


    public void setUriEncoding(String uriEncoding) {
        this.uriEncoding = uriEncoding;
    }
}
