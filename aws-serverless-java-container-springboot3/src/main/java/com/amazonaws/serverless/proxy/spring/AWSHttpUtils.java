package com.amazonaws.serverless.proxy.spring;

import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.function.serverless.web.ServerlessHttpServletRequest;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;
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

public class AWSHttpUtils {
	
	private static Log logger = LogFactory.getLog(AWSWebRuntimeEventLoop.class);
	
	
	public static AwsProxyResponse serviceAWS(String gatewayEvent, ServerlessMVC mvc, ObjectMapper mapper, AwsProxyHttpServletResponseWriter responseWriter) {
		HttpServletRequest request = AWSHttpUtils.generateHttpServletRequest(gatewayEvent, null, mvc.getServletContext(), mapper);
		CountDownLatch latch = new CountDownLatch(1);
        AwsHttpServletResponse response = new AwsHttpServletResponse(request, latch);
		try {
			mvc.service(request, response);
			latch.await(10, TimeUnit.SECONDS);
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
			String path = AWSHttpUtils.class.getProtectionDomain().getCodeSource().getLocation().toString();
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
				? AWSHttpUtils.generateRequest2(jsonRequest, lambdaContext, securityWriter, mapper, servletContext)
				: AWSHttpUtils.generateRequest1(jsonRequest, lambdaContext, securityWriter, mapper, servletContext);
		return httpServletRequest;
	}

	@SuppressWarnings({ "unchecked", "rawtypes" })
	private static HttpServletRequest generateRequest1(String request, Context lambdaContext,
			SecurityContextWriter securityWriter, ObjectMapper mapper, ServletContext servletContext) {
		AwsProxyRequest v1Request = readValue(request, AwsProxyRequest.class, mapper);
		
		ServerlessHttpServletRequest httpRequest = new ServerlessHttpServletRequest(servletContext, v1Request.getHttpMethod(), v1Request.getPath());
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
		
		v2Request.getHeaders().forEach((k,v) -> httpRequest.setHeader(k, v));
	
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
	
	private static <T> T readValue(String json, Class<T> clazz, ObjectMapper mapper) {
		try {
			return mapper.readValue(json, clazz);
		} 
		catch (Exception e) {
			throw new IllegalStateException(e);
		}
	}
	
//	public static class ProxyServletConfig implements ServletConfig {
//
//		private final ServletContext servletContext;
//
//		public ProxyServletConfig(ServletContext servletContext) {
//			this.servletContext = servletContext;
//		}
//
//		@Override
//		public String getServletName() {
//			return DispatcherServletAutoConfiguration.DEFAULT_DISPATCHER_SERVLET_BEAN_NAME;
//		}
//
//		@Override
//		public ServletContext getServletContext() {
//			return this.servletContext;
//		}
//
//		@Override
//		public Enumeration<String> getInitParameterNames() {
//			return Collections.enumeration(new ArrayList<String>());
//		}
//
//		@Override
//		public String getInitParameter(String name) {
//			return null;
//		}
//	}
}
