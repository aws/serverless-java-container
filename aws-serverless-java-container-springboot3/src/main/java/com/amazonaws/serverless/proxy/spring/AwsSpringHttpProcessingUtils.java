package com.amazonaws.serverless.proxy.spring;

import java.io.InputStream;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Base64;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

import org.apache.commons.io.Charsets;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.cloud.function.serverless.web.ServerlessHttpServletRequest;
import org.springframework.cloud.function.serverless.web.ServerlessMVC;
import org.springframework.http.HttpHeaders;
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

		populateQueryStringparameters(v1Request.getQueryStringParameters(), httpRequest);
		
		if (v1Request.getMultiValueHeaders() != null) {
			MultiValueMapAdapter headers = new MultiValueMapAdapter(v1Request.getMultiValueHeaders());
			httpRequest.setHeaders(headers);
		}
        if (StringUtils.hasText(v1Request.getBody())) {
			if (v1Request.getHeaders().get(HttpHeaders.CONTENT_TYPE)==null) {
				httpRequest.setContentType("application/json");
			}
            if (v1Request.isBase64Encoded()) {
                httpRequest.setContent(Base64.getMimeDecoder().decode(v1Request.getBody()));
            } else {
                Charset charseEncoding = parseCharacterEncoding(v1Request.getHeaders().get(HttpHeaders.CONTENT_TYPE));
                httpRequest.setContent(v1Request.getBody().getBytes(charseEncoding));
            }
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
			if (v2Request.getHeaders().get(HttpHeaders.CONTENT_TYPE)==null) {
				httpRequest.setContentType("application/json");
			}
			if (v2Request.isBase64Encoded()) {
                httpRequest.setContent(Base64.getMimeDecoder().decode(v2Request.getBody()));
            } else {
                Charset charseEncoding = parseCharacterEncoding(v2Request.getHeaders().get(HttpHeaders.CONTENT_TYPE));
                httpRequest.setContent(v2Request.getBody().getBytes(charseEncoding));
            }
        }
		httpRequest.setAttribute(RequestReader.HTTP_API_CONTEXT_PROPERTY, v2Request.getRequestContext());
		httpRequest.setAttribute(RequestReader.HTTP_API_STAGE_VARS_PROPERTY, v2Request.getStageVariables());
		httpRequest.setAttribute(RequestReader.HTTP_API_EVENT_PROPERTY, v2Request);
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

    static final String HEADER_KEY_VALUE_SEPARATOR = "=";
    static final String HEADER_VALUE_SEPARATOR = ";";
    static final String ENCODING_VALUE_KEY = "charset";
    static protected Charset parseCharacterEncoding(String contentTypeHeader) {
        // we only look at content-type because content-encoding should only be used for
        // "binary" requests such as gzip/deflate.
        Charset defaultCharset = StandardCharsets.UTF_8;
        if (contentTypeHeader == null) {
            return defaultCharset;
        }

        String[] contentTypeValues = contentTypeHeader.split(HEADER_VALUE_SEPARATOR);
        if (contentTypeValues.length <= 1) {
            return defaultCharset;
        }

        for (String contentTypeValue : contentTypeValues) {
            if (contentTypeValue.trim().startsWith(ENCODING_VALUE_KEY)) {
                String[] encodingValues = contentTypeValue.split(HEADER_KEY_VALUE_SEPARATOR);
                if (encodingValues.length <= 1) {
                    return defaultCharset;
                }
                try {
                    return Charsets.toCharset(encodingValues[1]);
                } catch (UnsupportedCharsetException ex) {
                    return defaultCharset;
                }
            }
        }
        return defaultCharset;
    }

}
