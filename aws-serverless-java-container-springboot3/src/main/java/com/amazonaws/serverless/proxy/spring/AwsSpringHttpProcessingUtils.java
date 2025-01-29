package com.amazonaws.serverless.proxy.spring;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import com.amazonaws.serverless.proxy.internal.HttpUtils;
import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.function.serverless.web.ServerlessHttpServletRequest;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.util.StringUtils;

import com.amazonaws.serverless.proxy.AwsHttpApiV2SecurityContextWriter;
import com.amazonaws.serverless.proxy.AwsProxySecurityContextWriter;
import com.amazonaws.serverless.proxy.RequestReader;
import com.amazonaws.serverless.proxy.SecurityContextWriter;
import com.amazonaws.serverless.proxy.internal.servlet.AwsHttpServletResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsProxyHttpServletResponseWriter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequest;
import com.amazonaws.services.lambda.runtime.Context;
import com.fasterxml.jackson.databind.ObjectMapper;

import jakarta.servlet.ServletContext;
import jakarta.servlet.http.HttpServletRequest;

class AwsSpringHttpProcessingUtils {
	
	private static Log logger = LogFactory.getLog(AwsSpringHttpProcessingUtils.class);
	private static final int LAMBDA_MAX_REQUEST_DURATION_MINUTES = 15;
	
	private AwsSpringHttpProcessingUtils() {
		
	}
	
	public static AwsProxyResponse processRequest(HttpServletRequest request, ServerlessMVC mvc, 
												  AwsProxyHttpServletResponseWriter responseWriter) {
		CountDownLatch latch = new CountDownLatch(1);
        AwsHttpServletResponse response = new AwsHttpServletResponse(request, latch);
		try {
			mvc.service(request, response);
			boolean requestTimedOut = !latch.await(LAMBDA_MAX_REQUEST_DURATION_MINUTES, TimeUnit.MINUTES); // timeout is potentially lower as user configures it
			if (requestTimedOut) {
				logger.warn("request timed out after " + LAMBDA_MAX_REQUEST_DURATION_MINUTES + " minutes");
			}
			AwsProxyResponse awsResponse = responseWriter.writeResponse(response, null);
			return awsResponse;
		} 
		catch (Exception e) {
			e.printStackTrace();
			throw new IllegalStateException(e);
		}
	}
	
	public static String extractVersion() {
		try {
			String path = AwsSpringHttpProcessingUtils.class.getProtectionDomain().getCodeSource().getLocation().toString();
			int endIndex = path.lastIndexOf('.');
			if (endIndex < 0) {
				return "UNKNOWN-VERSION";
			}
			int startIndex = path.lastIndexOf("/") + 1;
			return path.substring(startIndex, endIndex).replace("spring-cloud-function-serverless-web-", "");
		}
		catch (Exception e) {
			if (logger.isDebugEnabled()) {
				logger.debug("Failed to detect version", e);
			}
			return "UNKNOWN-VERSION";
		}

	}
	
	public static HttpServletRequest generateHttpServletRequest(InputStream jsonRequest, Context lambdaContext,
			ServletContext servletContext, ObjectMapper mapper) {
		try {
			String text = new String(FileCopyUtils.copyToByteArray(jsonRequest), StandardCharsets.UTF_8);
			if (logger.isDebugEnabled()) {
				logger.debug("Creating HttpServletRequest from: " + text);
			}
			return generateHttpServletRequest(text, lambdaContext, servletContext, mapper);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
	@SuppressWarnings({ "rawtypes", "unchecked" })
	public static HttpServletRequest generateHttpServletRequest(String jsonRequest, Context lambdaContext,
			ServletContext servletContext, ObjectMapper mapper) {
		Map<String, Object> _request = readValue(jsonRequest, Map.class, mapper);
		SecurityContextWriter securityWriter = "2.0".equals(_request.get("version"))
				? new AwsHttpApiV2SecurityContextWriter()
				: new AwsProxySecurityContextWriter();
		HttpServletRequest httpServletRequest = "2.0".equals(_request.get("version"))
				? AwsSpringHttpProcessingUtils.generateRequest2(jsonRequest, lambdaContext, securityWriter, mapper, servletContext)
				: AwsSpringHttpProcessingUtils.generateRequest1(jsonRequest, lambdaContext, securityWriter, mapper, servletContext);
		return httpServletRequest;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static HttpServletRequest generateRequest1(String request, Context lambdaContext,
			SecurityContextWriter securityWriter, ObjectMapper mapper, ServletContext servletContext) {
		AwsProxyRequest v1Request = readValue(request, AwsProxyRequest.class, mapper);
		
		ServerlessHttpServletRequest httpRequest = new ServerlessHttpServletRequest(servletContext, v1Request.getHttpMethod(), v1Request.getPath());

		populateQueryStringParameters(v1Request.getQueryStringParameters(), httpRequest);
		if (v1Request.getMultiValueQueryStringParameters() != null) {
			MultiValueMapAdapter<String, String> queryStringParameters = new MultiValueMapAdapter(v1Request.getMultiValueQueryStringParameters());
			queryStringParameters.forEach((k, v) -> httpRequest.setParameter(k, StringUtils.collectionToCommaDelimitedString(v)));
		}
		
		if (v1Request.getMultiValueHeaders() != null) {
			MultiValueMapAdapter headers = new MultiValueMapAdapter(v1Request.getMultiValueHeaders());
			httpRequest.setHeaders(headers);
		}
        populateContentAndContentType(
                v1Request.getBody(),
                v1Request.getMultiValueHeaders().getFirst(HttpHeaders.CONTENT_TYPE),
                v1Request.isBase64Encoded(),
                httpRequest
        );
		if (v1Request.getRequestContext() != null) {
			httpRequest.setAttribute(RequestReader.API_GATEWAY_CONTEXT_PROPERTY, v1Request.getRequestContext());
			httpRequest.setAttribute(RequestReader.ALB_CONTEXT_PROPERTY, v1Request.getRequestContext().getElb());
		}
		httpRequest.setAttribute(RequestReader.API_GATEWAY_STAGE_VARS_PROPERTY, v1Request.getStageVariables());
		httpRequest.setAttribute(RequestReader.API_GATEWAY_EVENT_PROPERTY, v1Request);
		httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
		httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY,
				securityWriter.writeSecurityContext(v1Request, lambdaContext));
		return httpRequest;
	}
	
	

	@SuppressWarnings({ "rawtypes", "unchecked" })
	private static HttpServletRequest generateRequest2(String request, Context lambdaContext,
			SecurityContextWriter securityWriter, ObjectMapper mapper, ServletContext servletContext) {
		HttpApiV2ProxyRequest v2Request = readValue(request, HttpApiV2ProxyRequest.class, mapper);
		
		
		ServerlessHttpServletRequest httpRequest = new ServerlessHttpServletRequest(servletContext,
				v2Request.getRequestContext().getHttp().getMethod(), v2Request.getRequestContext().getHttp().getPath());
		populateQueryStringParameters(v2Request.getQueryStringParameters(), httpRequest);
		
		v2Request.getHeaders().forEach(httpRequest::setHeader);

        populateContentAndContentType(
                v2Request.getBody(),
                v2Request.getHeaders().get(HttpHeaders.CONTENT_TYPE),
                v2Request.isBase64Encoded(),
                httpRequest
        );

		httpRequest.setAttribute(RequestReader.HTTP_API_CONTEXT_PROPERTY, v2Request.getRequestContext());
		httpRequest.setAttribute(RequestReader.HTTP_API_STAGE_VARS_PROPERTY, v2Request.getStageVariables());
		httpRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, v2Request);
		httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
		httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY,
				securityWriter.writeSecurityContext(v2Request, lambdaContext));
		return httpRequest;
	}
	
	private static void populateQueryStringParameters(Map<String, String> requestParameters, ServerlessHttpServletRequest httpRequest) {
		if (!CollectionUtils.isEmpty(requestParameters)) {
			for (Entry<String, String> entry : requestParameters.entrySet()) {
				httpRequest.setParameter(entry.getKey(), entry.getValue());
			}
		}
	}
	
	private static <T> T readValue(String json, Class<T> clazz, ObjectMapper mapper) {
		try {
			return mapper.readValue(json, clazz);
		} 
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

    private static void populateContentAndContentType(
            String body,
            String contentType,
            boolean base64Encoded,
            ServerlessHttpServletRequest httpRequest) {
        if (StringUtils.hasText(body)) {
            httpRequest.setContentType(contentType == null ? MediaType.APPLICATION_JSON_VALUE : contentType);
            if (base64Encoded) {
                httpRequest.setContent(Base64.getMimeDecoder().decode(body));
            } else {
                Charset charseEncoding = HttpUtils.parseCharacterEncoding(contentType,StandardCharsets.UTF_8);
                httpRequest.setContent(body.getBytes(charseEncoding));
            }
        }
    }



}
