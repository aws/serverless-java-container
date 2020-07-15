package com.amazonaws.serverless.proxy.spring.echoapp;

import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.amazonaws.serverless.proxy.spring.echoapp.model.SingleValueModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.util.MultiValueMap;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.multipart.MultipartResolver;
import org.springframework.web.multipart.commons.CommonsMultipartResolver;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;

import java.io.IOException;
import java.net.URI;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Enumeration;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Random;

import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.fromMethodCall;
import static org.springframework.web.servlet.mvc.method.annotation.MvcUriComponentsBuilder.on;


@RestController
@EnableWebMvc
@RequestMapping("/echo")
public class EchoResource {
    public static final String TEST_GENERATE_URI = "test";
    public static final String STRING_BODY = "Hello";
    public static final String EX_MESSAGE = "404 exception message";

    @Bean
    public MultipartResolver multipartResolver() {
        CommonsMultipartResolver multipartResolver = new CommonsMultipartResolver();
        multipartResolver.setMaxUploadSize(1000000);
        return multipartResolver;
    }

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
    public MapResponseModel echoQueryString(HttpServletRequest request, @RequestParam(value="nonexistent", required=false) String nonexistentParamValue) {
        MapResponseModel queryStrings = new MapResponseModel();
        for (String key : request.getParameterMap().keySet()) {
            queryStrings.addValue(key, request.getParameterMap().get(key)[0]);
        }

        return queryStrings;
    }

    @RequestMapping(path = "/multivalue-query-string", method = RequestMethod.GET)
    public MapResponseModel countMultivalueQueryParams(@RequestParam MultiValueMap<String, String> multipleParams) {
        MapResponseModel out =  new MapResponseModel();
        for (String v : multipleParams.get("multiple")) {
            out.addValue(v, "ok");
        }
        return out;
    }

    @RequestMapping(path = "/list-query-string", method = RequestMethod.GET)
    public SingleValueModel echoListQueryString(@RequestParam(value="list") List<String> valueList) {
        SingleValueModel value = new SingleValueModel();
        value.setValue(valueList.size() + "");
        return value;
    }

    @RequestMapping(path = "/authorizer-principal", method = RequestMethod.GET)
    public SingleValueModel echoAuthorizerPrincipal(HttpServletRequest context) {
        SingleValueModel valueModel = new SingleValueModel();
        AwsProxyRequestContext awsProxyRequestContext =
                (AwsProxyRequestContext) context.getAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY);
        valueModel.setValue(awsProxyRequestContext.getAuthorizer().getPrincipalId());

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

    @RequestMapping(path = "/request-URI", method = RequestMethod.GET)
    public SingleValueModel echoRequestURI(HttpServletRequest request) {
        SingleValueModel valueModel = new SingleValueModel();
        valueModel.setValue(request.getRequestURI());

        return valueModel;
    }

    @RequestMapping(path = "/request-url", method = RequestMethod.GET)
    public SingleValueModel echoRequestURL(HttpServletRequest request) {
        SingleValueModel valueModel = new SingleValueModel();
        valueModel.setValue(request.getRequestURL().toString());

        return valueModel;
    }

    @RequestMapping(path = "/request-body", method = RequestMethod.POST)
    public SingleValueModel helloForPopulatedBody(@RequestBody(required = false) Optional<String> input) {
        SingleValueModel valueModel = new SingleValueModel();
        if (input.isPresent() && !"null".equals(input.get())) {
            valueModel.setValue("true");
        }

        return valueModel;
    }

    @RequestMapping(path = "/encoded-request-uri/{encoded-var}", method = RequestMethod.GET)
    public SingleValueModel echoEncodedRequestUri(@PathVariable("encoded-var") String encodedVar) {
        SingleValueModel valueModel = new SingleValueModel();
        valueModel.setValue(encodedVar);

        return valueModel;
    }

    @RequestMapping(path = "/generate-uri", method = RequestMethod.GET)
    public SingleValueModel echoGeneratedResourceLink() {
        SingleValueModel valueModel = new SingleValueModel();

        URI personUri = fromMethodCall(on(EchoResource.class).echoEncodedRequestUri(TEST_GENERATE_URI)).build().toUri();

        valueModel.setValue(personUri.toString());

        return valueModel;
    }

    @RequestMapping(path = "/last-modified", method = RequestMethod.GET)
    public ResponseEntity<String> echoLastModified() {
        return ResponseEntity
                       .ok()
                       .lastModified(Instant.now().minus(1, ChronoUnit.DAYS).toEpochMilli())
                       .body(STRING_BODY);
    }

    @RequestMapping(path = "/attachment", method=RequestMethod.POST)
    public ResponseEntity<String> receiveFile(@RequestParam("testFile") MultipartFile file) throws IOException {
        String fileName = file.getName();
        byte[] fileContents = file.getBytes();

        return ResponseEntity.ok(fileName);
    }
}
