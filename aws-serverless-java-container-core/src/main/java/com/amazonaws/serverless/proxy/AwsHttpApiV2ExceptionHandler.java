package com.amazonaws.serverless.proxy;

import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.model.ErrorModel;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.fasterxml.jackson.core.JsonProcessingException;
import jakarta.ws.rs.InternalServerErrorException;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AwsHttpApiV2ExceptionHandler implements ExceptionHandler<APIGatewayV2HTTPResponse>{

    private Logger log = LoggerFactory.getLogger(AwsHttpApiV2ExceptionHandler.class);

    //-------------------------------------------------------------
    // Constants
    //-------------------------------------------------------------

    static final String INTERNAL_SERVER_ERROR = "Internal Server Error";
    static final String GATEWAY_TIMEOUT_ERROR = "Gateway timeout";


    //-------------------------------------------------------------
    // Variables - Private - Static
    //-------------------------------------------------------------

    private static final Map<String, List<String>> headers = new HashMap<>();

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------

    static {
        List<String> values = new ArrayList<>();
        values.add(MediaType.APPLICATION_JSON);
        headers.put(HttpHeaders.CONTENT_TYPE, values);
    }


    //-------------------------------------------------------------
    // Implementation - ExceptionHandler
    //-------------------------------------------------------------


    @Override
    public APIGatewayV2HTTPResponse handle(Throwable ex) {
        log.error("Called exception handler for:", ex);

        // adding a print stack trace in case we have no appender or we are running inside SAM local, where need the
        // output to go to the stderr.
        ex.printStackTrace();
        APIGatewayV2HTTPResponse responseEvent = new APIGatewayV2HTTPResponse();

        responseEvent.setMultiValueHeaders(headers);
        if (ex instanceof InvalidRequestEventException || ex instanceof InternalServerErrorException) {
            //return new APIGatewayProxyResponseEvent(500, headers, getErrorJson(INTERNAL_SERVER_ERROR));
            responseEvent.setBody(getErrorJson(INTERNAL_SERVER_ERROR));
            responseEvent.setStatusCode(500);
            return responseEvent;
        } else {
            responseEvent.setBody(getErrorJson(GATEWAY_TIMEOUT_ERROR));
            responseEvent.setStatusCode(502);
            return responseEvent;
        }
    }


    @Override
    public void handle(Throwable ex, OutputStream stream) throws IOException {
        APIGatewayV2HTTPResponse response = handle(ex);

        LambdaContainerHandler.getObjectMapper().writeValue(stream, response);
    }


    //-------------------------------------------------------------
    // Methods - Protected
    //-------------------------------------------------------------

    String getErrorJson(String message) {

        try {
            return LambdaContainerHandler.getObjectMapper().writeValueAsString(new ErrorModel(message));
        } catch (JsonProcessingException e) {
            log.error("Could not produce error JSON", e);
            return "{ \"message\": \"" + message + "\" }";
        }
    }
}
