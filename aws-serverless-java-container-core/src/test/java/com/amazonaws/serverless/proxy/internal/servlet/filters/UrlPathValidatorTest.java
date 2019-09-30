package com.amazonaws.serverless.proxy.internal.servlet.filters;

import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.FilterHolder;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import org.junit.Test;

import javax.servlet.FilterConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import java.io.IOException;
import java.util.Collections;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import static junit.framework.TestCase.assertEquals;
import static junit.framework.TestCase.fail;

public class UrlPathValidatorTest {
    @Test
    public void init_noConfig_setsDefaultStatusCode() {
        UrlPathValidator pathValidator = new UrlPathValidator();
        try {
            pathValidator.init(null);
            assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, pathValidator.getInvalidStatusCode());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Unexpected ServletException");
        }
    }

    @Test
    public void init_withConfig_setsCorrectStatusCode() {
        UrlPathValidator pathValidator = new UrlPathValidator();
        Map<String, String> params = new HashMap<>();
        params.put(UrlPathValidator.PARAM_INVALID_STATUS_CODE, "401");
        FilterConfig cnf = mockFilterConfig(params);
        try {
            pathValidator.init(cnf);
            assertEquals(401, pathValidator.getInvalidStatusCode());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Unexpected ServletException");
        }
    }

    @Test
    public void init_withWrongConfig_setsDefaultStatusCode() {
        UrlPathValidator pathValidator = new UrlPathValidator();
        Map<String, String> params = new HashMap<>();
        params.put(UrlPathValidator.PARAM_INVALID_STATUS_CODE, "hello");
        FilterConfig cnf = mockFilterConfig(params);
        try {
            pathValidator.init(cnf);
            assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, pathValidator.getInvalidStatusCode());
        } catch (ServletException e) {
            e.printStackTrace();
            fail("Unexpected ServletException");
        }
    }

    @Test
    public void doFilter_invalidRelativePathUri_setsDefaultStatusCode() {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(new AwsProxyRequestBuilder("../..", "GET").build(), null, null);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(req, null);
        UrlPathValidator pathValidator = new UrlPathValidator();
        try {
            pathValidator.init(null);
            pathValidator.doFilter(req, resp, null);
            assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, resp.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception");
        }
    }

    @Test
    public void doFilter_invalidUri_setsDefaultStatusCode() {
        AwsProxyHttpServletRequest req = new AwsProxyHttpServletRequest(new AwsProxyRequestBuilder("wonkyprotocol://˝Ó#\u0009", "GET").build(), null, null);
        AwsHttpServletResponse resp = new AwsHttpServletResponse(req, null);
        UrlPathValidator pathValidator = new UrlPathValidator();
        try {
            pathValidator.init(null);
            pathValidator.doFilter(req, resp, null);
            assertEquals(UrlPathValidator.DEFAULT_ERROR_CODE, resp.getStatus());
        } catch (Exception e) {
            e.printStackTrace();
            fail("Unexpected exception");
        }
    }


    private FilterConfig mockFilterConfig(Map<String, String> initParams) {
        return new FilterConfig() {
            @Override
            public String getFilterName() {
                return null;
            }

            @Override
            public ServletContext getServletContext() {
                return null;
            }

            @Override
            public String getInitParameter(String s) {
                return initParams.get(s);
            }

            @Override
            public Enumeration<String> getInitParameterNames() {
                return Collections.enumeration(initParams.keySet());
            }
        };
    }
}
