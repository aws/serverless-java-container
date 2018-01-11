package com.amazonaws.serverless.proxy;


import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;

import org.junit.Test;

import static org.junit.Assert.*;


public class RequestReaderTest {

    private static final String ORDERS_URL = "/orders";
    private static final String BASE_PATH_MAPPING = "svc1";

    private static final AwsProxyHttpServletRequestReader requestReader = new AwsProxyHttpServletRequestReader();

    @Test
    public void defaultConfig_doNotStripBasePath() {
        ContainerConfig config = ContainerConfig.defaultConfig();
        assertFalse(config.isStripBasePath());
        assertNull(config.getServiceBasePath());
    }

    @Test
    public void setServiceBasePath_addSlashes() {
        ContainerConfig config = new ContainerConfig();

        config.setServiceBasePath(BASE_PATH_MAPPING);
        assertEquals("/" + BASE_PATH_MAPPING, config.getServiceBasePath());

        config.setServiceBasePath(BASE_PATH_MAPPING + "/");
        assertEquals("/" + BASE_PATH_MAPPING, config.getServiceBasePath());
    }

    @Test
    public void requestReader_stripBasePath() {
        ContainerConfig config = ContainerConfig.defaultConfig();
        String requestPath = "/" + BASE_PATH_MAPPING + ORDERS_URL;

        String finalPath = requestReader.stripBasePath(requestPath, config);
        assertNotNull(finalPath);
        assertEquals(requestPath, finalPath);

        config.setStripBasePath(true);
        config.setServiceBasePath(BASE_PATH_MAPPING);
        finalPath = requestReader.stripBasePath(requestPath, config);
        assertNotNull(finalPath);
        assertEquals(ORDERS_URL, finalPath);

        finalPath = requestReader.stripBasePath(ORDERS_URL, config);
        assertNotNull(finalPath);
        assertEquals(ORDERS_URL, finalPath);
    }

    @Test
    public void requestReader_doubleBasePath() {
        ContainerConfig config = ContainerConfig.defaultConfig();
        config.setStripBasePath(true);
        config.setServiceBasePath(BASE_PATH_MAPPING);

        String finalPath = requestReader.stripBasePath("/" + BASE_PATH_MAPPING + "/" + BASE_PATH_MAPPING, config);
        assertNotNull(finalPath);
        assertEquals("/" + BASE_PATH_MAPPING, finalPath);

        finalPath = requestReader.stripBasePath("/custom/" + BASE_PATH_MAPPING, config);
        assertNotNull(finalPath);
        assertEquals("/custom/" + BASE_PATH_MAPPING, finalPath);

        finalPath = requestReader.stripBasePath(BASE_PATH_MAPPING, config);
        assertNotNull(finalPath);
        // the request path does not start with a "/", the comparison in the method should fail
        // and nothing should get replaced
        assertEquals(BASE_PATH_MAPPING, finalPath);
    }
}
