package com.amazonaws.serverless.proxy;


import com.amazonaws.serverless.exceptions.InvalidRequestEventException;
import com.amazonaws.serverless.exceptions.InvalidResponseObjectException;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.ErrorModel;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.Before;
import org.junit.Test;
import org.mockito.Mockito;

import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import java.io.*;


public class AwsProxyExceptionHandlerTest {
    private static final String INTERNAL_SERVER_ERROR_MESSAGE = "Internal server error";
    private static final String INVALID_REQUEST_MESSAGE = "Invalid request error";
    private static final String INVALID_RESPONSE_MESSAGE = "Invalid response error";
    private AwsProxyExceptionHandler exceptionHandler;
    private ObjectMapper objectMapper;


    @Before
    public void setUp() {
        exceptionHandler = new AwsProxyExceptionHandler();
        objectMapper = new ObjectMapper();
    }

    @Test
    public void typedHandle_InvalidRequestEventException_500State() {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null));

        assertNotNull(resp);
        assertEquals(500, resp.getStatusCode());
    }

    @Test
    public void typedHandle_InvalidRequestEventException_responseString()
            throws JsonProcessingException {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null));

        assertNotNull(resp);
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.INTERNAL_SERVER_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void typedHandle_InvalidRequestEventException_jsonContentTypeHeader() {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null));

        assertNotNull(resp);
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void typedHandle_InvalidResponseObjectException_502State() {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null));

        assertNotNull(resp);
        assertEquals(502, resp.getStatusCode());
    }

    @Test
    public void typedHandle_InvalidResponseObjectException_responseString()
            throws JsonProcessingException {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null));

        assertNotNull(resp);
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.GATEWAY_TIMEOUT_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void typedHandle_InvalidResponseObjectException_jsonContentTypeHeader() {
        AwsProxyResponse resp = exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null));

        assertNotNull(resp);
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void typedHandle_InternalServerErrorException_500State() {
        // Needed to mock InternalServerErrorException because it leverages RuntimeDelegate to set an internal
        // response object.
        InternalServerErrorException mockInternalServerErrorException = Mockito.mock(InternalServerErrorException.class);
        Mockito.when(mockInternalServerErrorException.getMessage()).thenReturn(INTERNAL_SERVER_ERROR_MESSAGE);

        AwsProxyResponse resp = exceptionHandler.handle(mockInternalServerErrorException);

        assertNotNull(resp);
        assertEquals(500, resp.getStatusCode());
    }

    @Test
    public void typedHandle_InternalServerErrorException_responseString()
            throws JsonProcessingException {
        InternalServerErrorException mockInternalServerErrorException = Mockito.mock(InternalServerErrorException.class);
        Mockito.when(mockInternalServerErrorException.getMessage()).thenReturn(INTERNAL_SERVER_ERROR_MESSAGE);

        AwsProxyResponse resp = exceptionHandler.handle(mockInternalServerErrorException);

        assertNotNull(resp);
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.INTERNAL_SERVER_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void typedHandle_InternalServerErrorException_jsonContentTypeHeader() {
        InternalServerErrorException mockInternalServerErrorException = Mockito.mock(InternalServerErrorException.class);
        Mockito.when(mockInternalServerErrorException.getMessage()).thenReturn(INTERNAL_SERVER_ERROR_MESSAGE);

        AwsProxyResponse resp = exceptionHandler.handle(mockInternalServerErrorException);

        assertNotNull(resp);
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void typedHandle_NullPointerException_responseObject()
            throws JsonProcessingException {
        AwsProxyResponse resp = exceptionHandler.handle(new NullPointerException());

        assertNotNull(resp);
        assertEquals(502, resp.getStatusCode());
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.GATEWAY_TIMEOUT_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void streamHandle_InvalidRequestEventException_500State()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        assertEquals(500, resp.getStatusCode());
    }

    @Test
    public void streamHandle_InvalidRequestEventException_responseString()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.INTERNAL_SERVER_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void streamHandle_InvalidRequestEventException_jsonContentTypeHeader()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidRequestEventException(INVALID_REQUEST_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void streamHandle_InvalidResponseObjectException_502State()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        assertEquals(502, resp.getStatusCode());
    }

    @Test
    public void streamHandle_InvalidResponseObjectException_responseString()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        String body = objectMapper.writeValueAsString(new ErrorModel(AwsProxyExceptionHandler.GATEWAY_TIMEOUT_ERROR));
        assertEquals(body, resp.getBody());
    }

    @Test
    public void streamHandle_InvalidResponseObjectException_jsonContentTypeHeader()
            throws IOException {
        ByteArrayOutputStream respStream = new ByteArrayOutputStream();
        exceptionHandler.handle(new InvalidResponseObjectException(INVALID_RESPONSE_MESSAGE, null), respStream);

        assertNotNull(respStream);
        assertTrue(respStream.size() > 0);
        AwsProxyResponse resp = objectMapper.readValue(new ByteArrayInputStream(respStream.toByteArray()), AwsProxyResponse.class);
        assertNotNull(resp);
        assertTrue(resp.getMultiValueHeaders().containsKey(HttpHeaders.CONTENT_TYPE));
        assertEquals(MediaType.APPLICATION_JSON, resp.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Test
    public void errorMessage_InternalServerError_staticString() {
        assertEquals("Internal Server Error", AwsProxyExceptionHandler.INTERNAL_SERVER_ERROR);
    }

    @Test
    public void errorMessage_GatewayTimeout_staticString() {
        assertEquals("Gateway timeout", AwsProxyExceptionHandler.GATEWAY_TIMEOUT_ERROR);
    }

    @Test
    public void getErrorJson_ErrorModel_validJson()
            throws IOException {
        String output = exceptionHandler.getErrorJson(INVALID_RESPONSE_MESSAGE);
        assertNotNull(output);
        ErrorModel error = objectMapper.readValue(output, ErrorModel.class);
        assertNotNull(error);
        assertEquals(INVALID_RESPONSE_MESSAGE, error.getMessage());
    }

    @Test
    public void getErrorJson_JsonParsinException_validJson()
            throws IOException {
        ObjectMapper mockMapper = mock(ObjectMapper.class);
        JsonProcessingException exception = mock(JsonProcessingException.class);
        when(mockMapper.writeValueAsString(any(Object.class))).thenThrow(exception);

        String output = exceptionHandler.getErrorJson(INVALID_RESPONSE_MESSAGE);
        assertNotNull(output);
        ErrorModel error = objectMapper.readValue(output, ErrorModel.class);
        assertNotNull(error);
        assertEquals(INVALID_RESPONSE_MESSAGE, error.getMessage());
    }
}
