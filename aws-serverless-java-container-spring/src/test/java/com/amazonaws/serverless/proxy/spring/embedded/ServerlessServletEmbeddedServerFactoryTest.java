package com.amazonaws.serverless.proxy.spring.embedded;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.spring.SpringBootLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.springbootapp.TestApplication;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.boot.context.embedded.EmbeddedServletContainer;
import org.springframework.boot.context.embedded.EmbeddedServletContainerException;
import org.springframework.boot.web.servlet.ServletContextInitializer;
import org.springframework.core.SpringVersion;

import javax.servlet.*;
import java.io.IOException;
import java.util.Objects;

import static org.junit.Assert.*;
import static org.junit.Assume.assumeFalse;

public class ServerlessServletEmbeddedServerFactoryTest {

    @BeforeClass
    public static void before() {
        // We skip the tests if we are running against Spring 5.2.x - SpringBoot 1.5 is deprecated and no longer
        // breaking changes in the latest Spring releases have not been supported in it.
        // TODO: Update the check to verify any Spring version above 5.2
        assumeFalse(Objects.requireNonNull(SpringVersion.getVersion()).startsWith("5.2"));
    }

    // initialize a container handler to that the embedded factory can grab its instnace
    private SpringBootLambdaContainerHandler h = SpringBootLambdaContainerHandler.getAwsProxyHandler(TestApplication.class);

    public ServerlessServletEmbeddedServerFactoryTest() throws ContainerInitializationException {
    }

    @Test
    public void getContainer_populatesInitializers() {
        ServerlessServletEmbeddedServerFactory factory = new ServerlessServletEmbeddedServerFactory();
        TestServlet initializer = new TestServlet(false);
        EmbeddedServletContainer container = factory.getEmbeddedServletContainer(initializer);
        assertNotNull(((ServerlessServletEmbeddedServerFactory)container).getInitializers());
        assertEquals(1, ((ServerlessServletEmbeddedServerFactory)container).getInitializers().length);
        assertEquals(initializer, ((ServerlessServletEmbeddedServerFactory)container).getInitializers()[0]);
        container.stop(); // calling stop just once to get the test coverage since there's no code in it
    }

    @Test
    public void start_throwsException() {
        ServerlessServletEmbeddedServerFactory factory = new ServerlessServletEmbeddedServerFactory();
        TestServlet initializer = new TestServlet(true);
        EmbeddedServletContainer container = factory.getEmbeddedServletContainer(initializer);
        try {
            container.start();
        } catch (EmbeddedServletContainerException e) {
            assertTrue(ServletException.class.isAssignableFrom(e.getCause().getClass()));
            assertEquals(TestServlet.EXCEPTION_MESSAGE, e.getCause().getMessage());
            return;
        }
        fail("Did not throw the expected exception");
    }

    @Test
    public void start_withoutException_setsServletContext() {
        ServerlessServletEmbeddedServerFactory factory = new ServerlessServletEmbeddedServerFactory();
        TestServlet initializer = new TestServlet(false);
        EmbeddedServletContainer container = factory.getEmbeddedServletContainer(initializer);
        container.start();
        assertNotNull(initializer.getCtx());
        assertEquals(h.getServletContext(), initializer.getCtx());
    }

    public static class TestServlet implements Servlet, ServletContextInitializer {
        private ServletContext ctx;
        private boolean throwOnInit;
        public static final String EXCEPTION_MESSAGE = "Throw on init";

        public TestServlet(boolean throwOnInit) {
            this.throwOnInit = throwOnInit;
        }

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

        @Override
        public void onStartup(ServletContext servletContext) throws ServletException {
            ctx = servletContext;
            if (throwOnInit) {
                throw new ServletException(EXCEPTION_MESSAGE);
            }
        }

        public ServletContext getCtx() {
            return ctx;
        }
    }
}
