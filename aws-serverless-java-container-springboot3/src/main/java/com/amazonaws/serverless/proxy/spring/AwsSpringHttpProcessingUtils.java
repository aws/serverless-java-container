package com.amazonaws.serverless.proxy.spring;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import com.amazonaws.serverless.proxy.*;
import com.amazonaws.serverless.proxy.model.VPCLatticeV2RequestEvent;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.function.serverless.web.ServerlessHttpServletRequest;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;
import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.MultiValueMapAdapter;
import org.springframework.util.StringUtils;

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

	public static HttpServletRequest generateLatticeV2HttpServletRequest(String jsonRequest, Context lambdaContext,
																		 ServletContext servletContext, ObjectMapper mapper) {
		SecurityContextWriter securityWriter = new AwsVPCLatticeV2SecurityContextWriter();
        return AwsSpringHttpProcessingUtils.generateRequestLatticeV2(jsonRequest, lambdaContext, securityWriter, mapper, servletContext);
	}

	public static HttpServletRequest generateLatticeV2HttpServletRequest(InputStream jsonRequest, Context lambdaContext,
																ServletContext servletContext, ObjectMapper mapper) {
		try {
			String text = new String(FileCopyUtils.copyToByteArray(jsonRequest), StandardCharsets.UTF_8);
			if (logger.isDebugEnabled()) {
				logger.debug("Creating HttpServletRequest from: " + text);
			}
			return generateLatticeV2HttpServletRequest(text, lambdaContext, servletContext, mapper);
		} catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static HttpServletRequest generateRequest1(String request, Context lambdaContext,
			SecurityContextWriter securityWriter, ObjectMapper mapper, ServletContext servletContext) {
		AwsProxyRequest v1Request = readValue(request, AwsProxyRequest.class, mapper);
		
		ServerlessHttpServletRequest httpRequest = new ServerlessHttpServletRequest(servletContext, v1Request.getHttpMethod(), v1Request.getPath());

		populateQueryStringparameters(v1Request.getQueryStringParameters(), httpRequest);
		
		if (v1Request.getMultiValueHeaders() != null) {
			MultiValueMapAdapter headers = new MultiValueMapAdapter(v1Request.getMultiValueHeaders());
			httpRequest.setHeaders(headers);
		}
		if (StringUtils.hasText(v1Request.getBody())) {
			httpRequest.setContentType("application/json");
			httpRequest.setContent(v1Request.getBody().getBytes(StandardCharsets.UTF_8));
		}
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
		populateQueryStringparameters(v2Request.getQueryStringParameters(), httpRequest);

		v2Request.getHeaders().forEach(httpRequest::setHeader);
		
		if (StringUtils.hasText(v2Request.getBody())) {
			httpRequest.setContentType("application/json");
			httpRequest.setContent(v2Request.getBody().getBytes(StandardCharsets.UTF_8));
		}
		httpRequest.setAttribute(RequestReader.HTTP_API_CONTEXT_PROPERTY, v2Request.getRequestContext());
		httpRequest.setAttribute(RequestReader.HTTP_API_STAGE_VARS_PROPERTY, v2Request.getStageVariables());
		httpRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, v2Request);
		httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
		httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY,
				securityWriter.writeSecurityContext(v2Request, lambdaContext));
		return httpRequest;
	}

	private static HttpServletRequest generateRequestLatticeV2(String request, Context lambdaContext,
															   SecurityContextWriter securityWriter, ObjectMapper mapper, ServletContext servletContext) {
		VPCLatticeV2RequestEvent v2Request = readValue(request, VPCLatticeV2RequestEvent.class, mapper);
		ServerlessHttpServletRequest httpRequest = new ServerlessHttpServletRequest(servletContext,
				v2Request.getMethod(), v2Request.getPath());
		populateQueryStringparameters(v2Request.getQueryStringParameters(), httpRequest);

		if (v2Request.getHeaders() != null) {
			MultiValueMapAdapter headers = new MultiValueMapAdapter(v2Request.getHeaders());
			httpRequest.setHeaders(headers);
		}

		if (StringUtils.hasText(v2Request.getBody())) {
			httpRequest.setContentType("application/json");
			httpRequest.setContent(v2Request.getBody().getBytes(StandardCharsets.UTF_8));
		}

		httpRequest.setAttribute(RequestReader.VPC_LATTICE_V2_CONTEXT_PROPERTY, v2Request.getRequestContext());
		httpRequest.setAttribute(RequestReader.VPC_LATTICE_V2_EVENT_PROPERTY, v2Request);
		httpRequest.setAttribute(RequestReader.LAMBDA_CONTEXT_PROPERTY, lambdaContext);
		httpRequest.setAttribute(RequestReader.JAX_SECURITY_CONTEXT_PROPERTY,
				securityWriter.writeSecurityContext(v2Request, lambdaContext));

		return httpRequest;
	}
	
	private static void populateQueryStringparameters(Map<String, String> requestParameters, ServerlessHttpServletRequest httpRequest) {
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

}
