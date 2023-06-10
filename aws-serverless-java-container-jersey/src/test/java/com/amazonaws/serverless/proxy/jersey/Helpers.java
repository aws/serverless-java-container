package com.amazonaws.serverless.proxy.jersey;

import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.events.APIGatewayProxyResponseEvent;
import com.amazonaws.services.lambda.runtime.events.APIGatewayV2HTTPResponse;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerResponseEvent;

import java.util.List;
import java.util.Map;
import java.util.Objects;

public class Helpers {

    public static AwsProxyResponse apiGwyToProxyResponse(APIGatewayProxyResponseEvent event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        if (Objects.nonNull(event.getIsBase64Encoded())) {
            proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        }

        if (Objects.nonNull(event.getStatusCode())) {
            proxyResponse.setStatusCode(event.getStatusCode());
        }

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }
        return proxyResponse;
    }
    public static AwsProxyResponse httpApiToProxyResponse(APIGatewayV2HTTPResponse event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        proxyResponse.setStatusCode(event.getStatusCode());

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }

        if (Objects.nonNull(event.getCookies())) {
            proxyResponse.setCookies(event.getCookies());
        }
        return proxyResponse;
    }
    public static AwsProxyResponse albToProxyResponse(ApplicationLoadBalancerResponseEvent event) {
        AwsProxyResponse proxyResponse = new AwsProxyResponse();
        if (Objects.nonNull(event.getBody())) {
            proxyResponse.setBody(event.getBody());
        }

        proxyResponse.setBase64Encoded(event.getIsBase64Encoded());
        proxyResponse.setStatusCode(event.getStatusCode());

        if (Objects.nonNull(event.getHeaders())) {
            proxyResponse.setHeaders(event.getHeaders());
        }

        if (Objects.nonNull(event.getStatusDescription())) {
            proxyResponse.setStatusDescription(event.getStatusDescription());
        }

        if (Objects.nonNull(event.getMultiValueHeaders())) {
            proxyResponse.setMultiValueHeaders(new Headers());

            for (Map.Entry<String, List<String>> header: event.getMultiValueHeaders().entrySet()) {
                proxyResponse.getMultiValueHeaders().put(header.getKey(), header.getValue());
            }
        }

        return proxyResponse;
    }
}
