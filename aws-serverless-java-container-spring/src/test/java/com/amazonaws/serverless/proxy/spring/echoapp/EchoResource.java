package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.proxy.internal.RequestReader;
import com.amazonaws.serverless.proxy.internal.model.ApiGatewayRequestContext;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.context.ServletContextAware;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import java.util.Enumeration;
import java.util.Map;
import java.util.Random;

@RestController
@EnableWebMvc
@RequestMapping("/echo")
public class EchoResource {
    @Autowired
    ServletContext servletContext;

    @RequestMapping(path = "/headers", method = RequestMethod.GET)
    public MapResponseModel echoHeaders(@RequestHeader Map<String, String> allHeaders) {
        MapResponseModel headers = new MapResponseModel();
        for (String key : allHeaders.keySet()) {
            headers.addValue(key, allHeaders.get(key));
        }

        return headers;
    }

    @RequestMapping(path = "/servlet-headers", method = RequestMethod.GET)
    public MapResponseModel echoServletHeaders(HttpServletRequest context) {
        MapResponseModel headers = new MapResponseModel();
        Enumeration<String> headerNames = context.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String headerName = headerNames.nextElement();
            headers.addValue(headerName, context.getHeader(headerName));
        }

        return headers;
    }

    @RequestMapping(path = "/query-string", method = RequestMethod.GET)
    public MapResponseModel echoQueryString(HttpServletRequest request) {
        MapResponseModel queryStrings = new MapResponseModel();
        for (String key : request.getParameterMap().keySet()) {
            queryStrings.addValue(key, request.getParameterMap().get(key)[0]);
        }

        return queryStrings;
    }

    @RequestMapping(path = "/authorizer-principal", method = RequestMethod.GET)
    public SingleValueModel echoAuthorizerPrincipal(HttpServletRequest context) {
        SingleValueModel valueModel = new SingleValueModel();
        ApiGatewayRequestContext apiGatewayRequestContext =
                (ApiGatewayRequestContext) context.getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(apiGatewayRequestContext.getAuthorizer().getPrincipalId());

        return valueModel;
    }

    @RequestMapping(path = "/json-body", method = RequestMethod.POST, consumes = { "application/json" })
    public SingleValueModel echoJsonValue(@RequestBody SingleValueModel requestValue) {
        SingleValueModel output = new SingleValueModel();
        output.setValue(requestValue.getValue());

        return output;
    }

    @RequestMapping(path = "/status-code", method = RequestMethod.GET)
    public ResponseEntity<SingleValueModel> echoCustomStatusCode(@RequestParam("status") int statusCode ) {
        SingleValueModel output = new SingleValueModel();
        output.setValue("" + statusCode);

        return new ResponseEntity<SingleValueModel>(output, HttpStatus.valueOf(statusCode));
    }

    @RequestMapping(path = "/binary", method = RequestMethod.GET)
    public ResponseEntity<byte[]> echoBinaryData() {
        byte[] b = new byte[128];
        new Random().nextBytes(b);

        return new ResponseEntity<byte[]>(b, HttpStatus.OK);
    }

    @RequestMapping(path = "/servlet-context", method=RequestMethod.GET)
    public ResponseEntity<String> getContext() {
        return new ResponseEntity<String>(this.servletContext.getServerInfo(), HttpStatus.OK);
    }
}
