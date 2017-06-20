package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.CustomHeaderFilter;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.ValidatedUserModel;
import org.junit.BeforeClass;
import org.junit.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import javax.ws.rs.core.MediaType;

import java.util.EnumSet;

import static org.junit.Assert.*;

// we don't use the spring annotations to pretend we are running in the actual container
public class SpringServletContextTest {
    private static final String STAGE = LambdaContainerHandler.SERVER_INFO + "/" + AwsServletContext.SERVLET_API_MAJOR_VERSION + "." + AwsServletContext.SERVLET_API_MINOR_VERSION;
    private MockLambdaContext lambdaContext = new MockLambdaContext();

    private static SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @BeforeClass
    public static void setUp() {
        try {
            handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
            handler.setRefreshContext(true);
            handler.onStartup(c -> {
                FilterRegistration.Dynamic registration = c.addFilter("CustomHeaderFilter", CustomHeaderFilter.class);
                // update the registration to map to a path
                registration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
                // servlet name mappings are disabled and will throw an exception
            });
        } catch (ContainerInitializationException e) {
            e.printStackTrace();
            fail();
        }
    }

    @Test
    public void context_autowireValidContext_echoContext() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/servlet-context", "GET")
                .json()
                .stage(STAGE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("text/plain", output.getHeaders().get("Content-Type").split(";")[0]);
        assertEquals(STAGE, output.getBody());
    }

    @Test
    public void context_contextAware_contextEcho() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/context/echo", "GET")
                .json()
                .stage(STAGE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("text/plain", output.getHeaders().get("Content-Type").split(";")[0]);
        assertEquals(STAGE, output.getBody());
    }

    @Test
    public void filter_customHeaderFilter_echoHeaders() {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/echo/headers", "GET")
                .json()
                .stage(STAGE)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertNotNull(output.getHeaders());
        assertTrue(output.getHeaders().size() > 0);
        assertNotNull(output.getHeaders().get(CustomHeaderFilter.HEADER_NAME));
        assertEquals(CustomHeaderFilter.HEADER_VALUE, output.getHeaders().get(CustomHeaderFilter.HEADER_NAME));
    }

    @Test
    public void filter_validationFilter_emptyName() {
        ValidatedUserModel userModel = new ValidatedUserModel();
        userModel.setFirstName("Test");
        AwsProxyRequest request = new AwsProxyRequestBuilder("/context/user", "POST")
                .json()
                .body(userModel)
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(HttpStatus.BAD_REQUEST.value(), output.getStatusCode());
    }
}

