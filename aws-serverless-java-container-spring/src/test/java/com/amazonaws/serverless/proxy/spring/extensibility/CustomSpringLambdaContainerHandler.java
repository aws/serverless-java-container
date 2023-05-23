package com.amazonaws.serverless.proxy.spring.extensibility;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyRequestEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import org.springframework.web.context.ConfigurableWebApplicationContext;

import jakarta.servlet.ServletRegistration;
import jakarta.servlet.http.HttpServletRequest;

public class CustomSpringLambdaContainerHandler<RequestType, ResponseType> extends SpringLambdaContainerHandler<RequestType, ResponseType> {

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects
     * @param config A set of classes annotated with the Spring @Configuration annotation
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getAwsProxyHandler(Class<?>... config) throws ContainerInitializationException {
        return new CustomSpringProxyHandlerBuilder<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>()
                .defaultProxy()
                .initializationWrapper(new InitializationWrapper())
                .configurationClasses(config)
                .buildAndInitialize();
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `AwsProxyRequest` and `AwsProxyResponse` objects and sets the given profiles as active
     * @param applicationContext A custom ConfigurableWebApplicationContext to be used
     * @param profiles The spring profiles to activate
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent> getAwsProxyHandler(ConfigurableWebApplicationContext applicationContext, String... profiles)
            throws ContainerInitializationException {
        return new CustomSpringProxyHandlerBuilder<APIGatewayProxyRequestEvent, APIGatewayProxyResponseEvent>()
                .defaultProxy()
                .initializationWrapper(new InitializationWrapper())
                .springApplicationContext(applicationContext)
                .profiles(profiles)
                .buildAndInitialize();
    }

    /**
     * Creates a default SpringLambdaContainerHandler initialized with the `APIGatewayV2HTTPEvent` and `AwsProxyResponse` objects
     * @param config A set of classes annotated with the Spring @Configuration annotation
     * @return An initialized instance of the `SpringLambdaContainerHandler`
     * @throws ContainerInitializationException When the Spring framework fails to start.
     */
    public static SpringLambdaContainerHandler<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse> getHttpApiV2ProxyHandler(Class<?>... config) throws ContainerInitializationException {
        return new CustomSpringProxyHandlerBuilder<APIGatewayV2HTTPEvent, APIGatewayV2HTTPResponse>()
                .defaultHttpApiV2Proxy()
                .initializationWrapper(new InitializationWrapper())
                .configurationClasses(config)
                .buildAndInitialize();
    }

    /**
     * Creates a new container handler with the given reader and writer objects
     *
     * @param requestTypeClass The class for the incoming Lambda event
     * @param requestReader An implementation of `RequestReader`
     * @param responseWriter An implementation of `ResponseWriter`
     * @param securityContextWriter An implementation of `SecurityContextWriter`
     * @param exceptionHandler An implementation of `ExceptionHandler`
     */
    public CustomSpringLambdaContainerHandler(Class<RequestType> requestTypeClass,
                                              Class<ResponseType> responseTypeClass,
                                              RequestReader<RequestType, HttpServletRequest> requestReader,
                                              ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                              SecurityContextWriter<RequestType> securityContextWriter,
                                              ExceptionHandler<ResponseType> exceptionHandler,
                                              ConfigurableWebApplicationContext applicationContext,
                                              InitializationWrapper init) {
        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter,
                exceptionHandler, applicationContext, init);
    }

    @Override
    protected void registerServlets() {
        CustomServlet customServlet = new CustomServlet();
        customServlet.setAppCtx(appContext);
        ServletRegistration.Dynamic reg = getServletContext().addServlet("customServlet", customServlet);
        reg.addMapping("/");
        reg.setLoadOnStartup(1);
    }

}
