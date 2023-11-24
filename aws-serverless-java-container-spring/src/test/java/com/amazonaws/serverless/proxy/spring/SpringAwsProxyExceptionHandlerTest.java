package com.amazonaws.serverless.proxy.spring;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.web.servlet.NoHandlerFoundException;

import static org.junit.jupiter.api.Assertions.assertEquals;

public class SpringAwsProxyExceptionHandlerTest {

    @Test
    void noHandlerFoundExceptionResultsIn404() {
        AwsProxyResponse response = new SpringAwsProxyExceptionHandler().
                handle(new NoHandlerFoundException(HttpMethod.GET.name(), "https://atesturl",
                        HttpHeaders.EMPTY));
        assertEquals(Response.Status.NOT_FOUND.getStatusCode(), response.getStatusCode());
    }
}
