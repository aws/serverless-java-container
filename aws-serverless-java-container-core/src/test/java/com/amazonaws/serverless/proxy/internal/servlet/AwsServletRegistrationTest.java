package com.amazonaws.serverless.proxy.internal.servlet;

import org.junit.Test;

import javax.servlet.*;

import java.io.IOException;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

public class AwsServletRegistrationTest {

    @Test
    public void getMappings_singleMapping_savedCorrectly() {
        ServletRegistration.Dynamic reg = new AwsServletRegistration("test", null, new AwsServletContext(null));
        reg.addMapping("/");
        assertEquals(1, reg.getMappings().size());
        Set<String> invalidMappings = reg.addMapping("/");
        assertEquals(1, invalidMappings.size());
        assertEquals("/", invalidMappings.toArray(new String[]{})[0]);
        reg.addMapping("/hello", "/world");
        assertEquals(3, reg.getMappings().size());
    }

    @Test
    public void metadata_savedAndReturnedCorrectly() {
        ServletRegistration.Dynamic reg = new AwsServletRegistration("test", null, new AwsServletContext(null));
        assertEquals("test", reg.getName());
        reg.setLoadOnStartup(2);
        assertEquals(2, ((AwsServletRegistration)reg).getLoadOnStartup());
        assertNull(reg.getRunAsRole());
        reg.setRunAsRole("role");
        assertEquals("role", reg.getRunAsRole());
        reg.setAsyncSupported(true);
        assertTrue(((AwsServletRegistration)reg).isAsyncSupported());
    }

    @Test
    public void setInitParameter_savedCorrectly() {
        ServletRegistration.Dynamic reg = new AwsServletRegistration("test", null, new AwsServletContext(null));
        assertTrue(reg.setInitParameter("param", "value"));
        assertFalse(reg.setInitParameter("param", "value"));
        Map<String, String> params = new HashMap<>();
        params.put("param2", "value2");
        params.put("param", "value");
        Set<String> invalidParams = reg.setInitParameters(params);
        assertEquals(1, invalidParams.size());
        assertEquals("param", invalidParams.toArray(new String[]{})[0]);
        assertEquals(2, reg.getInitParameters().size());
        assertEquals("value2", reg.getInitParameter("param2"));
    }

    @Test
    public void servletConfig_populatesConfig() throws ServletException {
        AwsServletContext servletCtx = new AwsServletContext(null);
        TestServlet servlet = new TestServlet();
        ServletRegistration.Dynamic reg = new AwsServletRegistration("test", servlet, servletCtx);
        assertEquals(servlet, ((AwsServletRegistration)reg).getServlet());
        Map<String, String> params = new HashMap<>();
        params.put("param2", "value2");
        params.put("param", "value");
        Set<String> invalidParams = reg.setInitParameters(params);
        assertEquals(0, invalidParams.size());
        ServletConfig config = ((AwsServletRegistration)reg).getServletConfig();
        assertNotNull(config);
        assertEquals("test", config.getServletName());
        assertEquals(servletCtx, config.getServletContext());
        int paramCnt = 0;
        Enumeration<String> paramNames = config.getInitParameterNames();
        while (paramNames.hasMoreElements()) {
            paramNames.nextElement();
            paramCnt++;
        }
        assertEquals(2, paramCnt);

    }

    private class TestServlet implements Servlet {

        @Override
        public void init(ServletConfig servletConfig) throws ServletException {

        }

        @Override
        public ServletConfig getServletConfig() {
            return null;
        }

        @Override
        public void service(ServletRequest servletRequest, ServletResponse servletResponse) throws ServletException, IOException {

        }

        @Override
        public String getServletInfo() {
            return null;
        }

        @Override
        public void destroy() {

        }
    }
}
