package com.amazonaws.serverless.proxy.struts2;

import com.amazonaws.serverless.exceptions.ContainerInitializationException;
import com.amazonaws.serverless.proxy.AwsProxyExceptionHandler;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.ExceptionHandler;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.ResponseWriter;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsLambdaServletContainerHandler;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequest;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletRequestReader;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.internal.testutils.Timer;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.services.lambda.runtime.Context;
import org.apache.struts2.dispatcher.filter.StrutsPrepareAndExecuteFilter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.servlet.DispatcherType;
import javax.servlet.FilterRegistration;
import java.util.EnumSet;
import java.util.concurrent.CountDownLatch;

/**
 * A Lambda handler to initialize the Struts2 filter and proxy the requests.
 *
 * @param <RequestType>  request type
 * @param <ResponseType> response type
 */
public class Struts2LambdaContainerHandler<RequestType, ResponseType> extends AwsLambdaServletContainerHandler<RequestType, ResponseType, AwsProxyHttpServletRequest, AwsHttpServletResponse> {

    private static final Logger log = LoggerFactory.getLogger(Struts2LambdaContainerHandler.class);

    public static final String HEADER_STRUTS_STATUS_CODE = "X-Struts-StatusCode";

    private static final String TIMER_STRUTS_2_CONTAINER_CONSTRUCTOR = "STRUTS2_CONTAINER_CONSTRUCTOR";
    private static final String TIMER_STRUTS_2_HANDLE_REQUEST = "STRUTS2_HANDLE_REQUEST";
    private static final String TIMER_STRUTS_2_COLD_START_INIT = "STRUTS2_COLD_START_INIT";
    private static final String STRUTS_FILTER_NAME = "Struts2Filter";

    private boolean initialized;

    public static Struts2LambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> getAwsProxyHandler() {
        return new Struts2LambdaContainerHandler(
                AwsProxyRequest.class,
                AwsProxyResponse.class,
                new AwsProxyHttpServletRequestReader(),
                new AwsProxyHttpServletResponseWriter(),
                new AwsProxySecurityContextWriter(),
                new AwsProxyExceptionHandler());
    }

    public Struts2LambdaContainerHandler(Class<RequestType> requestTypeClass,
                                         Class<ResponseType> responseTypeClass,
                                         RequestReader<RequestType, AwsProxyHttpServletRequest> requestReader,
                                         ResponseWriter<AwsHttpServletResponse, ResponseType> responseWriter,
                                         SecurityContextWriter<RequestType> securityContextWriter,
                                         ExceptionHandler<ResponseType> exceptionHandler) {

        super(requestTypeClass, responseTypeClass, requestReader, responseWriter, securityContextWriter, exceptionHandler);
        Timer.start(TIMER_STRUTS_2_CONTAINER_CONSTRUCTOR);
        this.initialized = false;
        Timer.stop(TIMER_STRUTS_2_CONTAINER_CONSTRUCTOR);
    }

    protected AwsHttpServletResponse getContainerResponse(AwsProxyHttpServletRequest request, CountDownLatch latch) {
        return new AwsHttpServletResponse(request, latch);
    }

    @Override
    protected void handleRequest(AwsProxyHttpServletRequest httpServletRequest,
                                 AwsHttpServletResponse httpServletResponse,
                                 Context lambdaContext) throws Exception {
        Timer.start(TIMER_STRUTS_2_HANDLE_REQUEST);
        if (!this.initialized) {
            initialize();
        }

        httpServletRequest.setServletContext(this.getServletContext());
        this.doFilter(httpServletRequest, httpServletResponse, null);
        String responseStatusCode = httpServletResponse.getHeader(HEADER_STRUTS_STATUS_CODE);
        if (responseStatusCode != null) {
            httpServletResponse.setStatus(Integer.parseInt(responseStatusCode));
        }
        Timer.stop(TIMER_STRUTS_2_HANDLE_REQUEST);
    }

    @Override
    public void initialize() throws ContainerInitializationException {
        log.info("Initialize Struts2 Lambda Application ...");
        Timer.start(TIMER_STRUTS_2_COLD_START_INIT);
        try {
            if (this.startupHandler != null) {
                this.startupHandler.onStartup(this.getServletContext());
            }
            StrutsPrepareAndExecuteFilter filter = new StrutsPrepareAndExecuteFilter();
            FilterRegistration.Dynamic filterRegistration = this.getServletContext()
                    .addFilter(STRUTS_FILTER_NAME, filter);
            filterRegistration.addMappingForUrlPatterns(EnumSet.of(DispatcherType.REQUEST), true, "/*");
        } catch (Exception e) {
            throw new ContainerInitializationException("Could not initialize Struts2", e);
        }

        this.initialized = true;
        Timer.stop(TIMER_STRUTS_2_COLD_START_INIT);
        log.info("... initialize of Struts2 Lambda Application completed!");
    }
}
