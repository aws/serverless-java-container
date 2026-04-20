package com.amazonaws.serverless.proxy.spring;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.InitializationWrapper;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.servletapp.ServletApplication;
import com.amazonaws.serverless.proxy.spring.webfluxapp.WebFluxTestApplication;
import java.util.Collection;
import java.util.List;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.junit.jupiter.params.provider.MethodSource;
import org.mockito.Mockito;
import org.springframework.boot.WebApplicationType;
import org.springframework.boot.builder.SpringApplicationBuilder;

class SpringBootLambdaContainerHandlerTest {

    SpringBootLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;
    SpringApplicationBuilder springApplicationBuilder;

    public static Collection<TestData> data() {
        return List.of(new TestData(WebApplicationType.SERVLET, ServletApplication.class),
            new TestData(WebApplicationType.REACTIVE, WebFluxTestApplication.class));
    }

    private void initSpringBootLambdaContainerHandlerTest(Class<?> springBootInitializer,
        WebApplicationType applicationType) {
        handler = Mockito.spy(new SpringBootLambdaContainerHandler<>(AwsProxyRequest.class,
            AwsProxyResponse.class,
            new AwsProxyHttpServletRequestReader(),
            new AwsProxyHttpServletResponseWriter(),
            new AwsProxySecurityContextWriter(),
            new AwsProxyExceptionHandler(),
            springBootInitializer,
            new InitializationWrapper(),
            applicationType));

        doAnswer(d -> {
            springApplicationBuilder = ((SpringApplicationBuilder) Mockito.spy(d.callRealMethod()));
            return springApplicationBuilder;
        }).when(handler).getSpringApplicationBuilder(any(Class[].class));
    }

    @ParameterizedTest
    @MethodSource("data")
    void initialize_withSpringBootInitializer(TestData data) throws ContainerInitializationException {
        initSpringBootLambdaContainerHandlerTest(data.springBootApplication(), data.applicationType());
        handler.initialize();

        verify(springApplicationBuilder, times(1)).main(data.springBootApplication());
    }

    @ParameterizedTest
    @EnumSource(WebApplicationType.class)
    void initialize_withoutSpringBootInitializer(WebApplicationType webApplicationType) {
        initSpringBootLambdaContainerHandlerTest(null, webApplicationType);
        assertThrows(IllegalArgumentException.class, handler::initialize, "Source must not be null");

        verify(springApplicationBuilder, never()).main(any());
    }

    record TestData(WebApplicationType applicationType, Class<?> springBootApplication) {}
}