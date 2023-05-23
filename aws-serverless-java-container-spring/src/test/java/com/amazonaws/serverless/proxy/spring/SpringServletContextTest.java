package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.echoapp.ContextResource;
import com.amazonaws.serverless.proxy.spring.echoapp.CustomHeaderFilter;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.ValidatedUserModel;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;

import jakarta.servlet.DispatcherType;
import jakarta.servlet.FilterRegistration;

import java.util.EnumSet;

import static org.junit.jupiter.api.Assertions.*;

// we don't use the spring annotations to pretend we are running in the actual container
public class SpringServletContextTest {
    private static final String STAGE = LambdaContainerHandler.SERVER_INFO + "/" + AwsServletContext.SERVLET_API_MAJOR_VERSION + "." + AwsServletContext.SERVLET_API_MINOR_VERSION;
    private MockLambdaContext lambdaContext = new MockLambdaContext();

    private static SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> handler;

    @BeforeAll
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
    void context_autowireValidContext_echoContext() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/echo/servlet-context", "GET")
                .json()
                .stage(STAGE)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("text/plain", output.getMultiValueHeaders().get("Content-Type").get(0).split(";")[0]);
        assertEquals(STAGE, output.getBody());
    }

    @Test
    void context_contextAware_contextEcho() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/context/echo", "GET")
                .json()
                .stage(STAGE)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());
        assertEquals("text/plain", output.getMultiValueHeaders().get("Content-Type").get(0).split(";")[0]);
        assertEquals(STAGE, output.getBody());
    }

    @Test
    void filter_customHeaderFilter_echoHeaders() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/echo/headers", "GET")
                .json()
                .stage(STAGE)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);
        assertNotNull(output.getMultiValueHeaders());
        assertTrue(output.getMultiValueHeaders().size() > 0);
        assertNotNull(output.getMultiValueHeaders().get(CustomHeaderFilter.HEADER_NAME));
        assertEquals(CustomHeaderFilter.HEADER_VALUE, output.getMultiValueHeaders().get(CustomHeaderFilter.HEADER_NAME).get(0));
    }

    @Test
    void filter_validationFilter_emptyName() {
        ValidatedUserModel userModel = new ValidatedUserModel();
        userModel.setFirstName("Test");
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/context/user", "POST")
                .json()
                .body(userModel)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);
        assertEquals(HttpStatus.BAD_REQUEST.value(), output.getStatusCode());
    }

    @Test
    void exception_populatedException_annotationValuesMappedCorrectly() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/context/exception", "GET")
                .stage(STAGE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);

        assertEquals(409, output.getStatusCode());
        assertTrue(output.getBody().contains(ContextResource.EXCEPTION_REASON));
    }

    @Test
    void cookie_injectInResponse_expectCustomSetCookie() {
        APIGatewayProxyRequestEvent request = new AwsProxyRequestBuilder("/context/cookie", "GET")
                .stage(STAGE)
                .header(HttpHeaders.ACCEPT, MediaType.APPLICATION_JSON_VALUE)
                .build();

        APIGatewayProxyResponseEvent output = handler.proxy(request, lambdaContext);


        assertEquals(200, output.getStatusCode());
        assertTrue(output.getMultiValueHeaders().containsKey(HttpHeaders.SET_COOKIE));
        assertTrue(output.getMultiValueHeaders().get(HttpHeaders.SET_COOKIE).get(0).contains(ContextResource.COOKIE_NAME + "=" + ContextResource.COOKIE_VALUE));
        assertTrue(output.getMultiValueHeaders().get(HttpHeaders.SET_COOKIE).get(0).contains(ContextResource.COOKIE_DOMAIN));
    }
}

