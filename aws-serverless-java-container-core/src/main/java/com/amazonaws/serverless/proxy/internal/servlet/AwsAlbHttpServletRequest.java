package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.ContainerConfig;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.services.lambda.runtime.Context;
import com.amazonaws.services.lambda.runtime.events.ApplicationLoadBalancerRequestEvent;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.servlet.http.Part;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.Principal;
import java.util.*;

public class AwsAlbHttpServletRequest extends AwsHttpServletRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------

    private ApplicationLoadBalancerRequestEvent request;
    private SecurityContext securityContext;
    private AwsAsyncContext asyncContext;
    private static Logger log = LoggerFactory.getLogger(AwsProxyHttpServletRequest.class);
    private ContainerConfig config;

    public AwsAlbHttpServletRequest(ApplicationLoadBalancerRequestEvent albRequest, Context lambdaContext, SecurityContext awsSecurityContext) {
        this(albRequest, lambdaContext, awsSecurityContext, LambdaContainerHandler.getContainerConfig());
    }

    public AwsAlbHttpServletRequest(ApplicationLoadBalancerRequestEvent albRequest, Context lambdaContext, SecurityContext awsSecurityContext, ContainerConfig config) {
        super(lambdaContext);
        this.request = albRequest;
        this.securityContext = awsSecurityContext;
        this.config = config;
    }

    public ApplicationLoadBalancerRequestEvent getAwsProxyRequest() {
        return this.request;
    }
    @Override
    public String getAuthType() {
        return securityContext.getAuthenticationScheme();
    }


    @Override
    public Cookie[] getCookies() {
        return AwsHttpServletRequestHelper.getCookies(request.getMultiValueHeaders());
    }


    @Override
    public long getDateHeader(String s) {
        return AwsHttpServletRequestHelper.getDateHeader(request.getMultiValueHeaders(), s, log);
    }


    @Override
    public String getHeader(String s) {
        return AwsHttpServletRequestHelper.getHeader(
                request.getMultiValueHeaders(),
                null,
                null,
                s,
                getHeaderValues(s)
        );
    }


    @Override
    public Enumeration<String> getHeaders(String s) {
        return AwsHttpServletRequestHelper.getHeaders(request.getMultiValueHeaders(), s);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return AwsHttpServletRequestHelper.getHeaderNames(request.getMultiValueHeaders());
    }


    @Override
    public int getIntHeader(String s) {
        return AwsHttpServletRequestHelper.getIntHeader(request.getMultiValueHeaders(), s);
    }


    @Override
    public String getMethod() {
        return request.getHttpMethod();
    }


    @Override
    public String getPathInfo() {
        return AwsHttpServletRequestHelper.getPathInfo(request.getPath());
    }


    @Override
    public String getPathTranslated() {
        return AwsHttpServletRequestHelper.getPathTranslated();
    }


    @Override
    public String getContextPath() {
        return AwsHttpServletRequestHelper.getContextPath(config, null, this);
    }


    @Override
    public String getQueryString() {
        return AwsHttpServletRequestHelper.getQueryString(request.getMultiValueQueryStringParameters(), config, log, this);
    }


    @Override
    public String getRemoteUser() {
        return AwsHttpServletRequestHelper.getRemoteUser(securityContext);
    }


    @Override
    public boolean isUserInRole(String s) {
        // TODO: Not supported?
        return false;
    }


    @Override
    public Principal getUserPrincipal() {
        return AwsHttpServletRequestHelper.getUserPrincipal(securityContext);
    }


    @Override
    public String getRequestURI() { return AwsHttpServletRequestHelper.getRequestURI(request.getPath(), this);}


    @Override
    public StringBuffer getRequestURL() {
        return AwsHttpServletRequestHelper.getRequestURL(request.getPath(), this);
    }


    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse)
            throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public void login(String s, String s1)
            throws ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public void logout()
            throws ServletException {
        throw new UnsupportedOperationException();
    }


    @Override
    public Collection<Part> getParts()
            throws IOException, ServletException {
        return AwsHttpServletRequestHelper.getParts(this);
    }


    @Override
    public Part getPart(String s)
            throws IOException, ServletException {
        return AwsHttpServletRequestHelper.getPart(s, this);
    }


    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass)
            throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    //-------------------------------------------------------------
    // Implementation - ServletRequest
    //-------------------------------------------------------------


    @Override
    public String getCharacterEncoding() {
        return AwsHttpServletRequestHelper.getCharacterEncoding(
                request.getMultiValueHeaders(),
                config,
                this
        );
    }


    @Override
    public void setCharacterEncoding(String s)
            throws UnsupportedEncodingException {
        if (request.getMultiValueHeaders() == null) {
            request.setMultiValueHeaders(new Headers());
        }
        String currentContentType = getFirst(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE);
        if (currentContentType == null || "".equals(currentContentType)) {
            log.debug("Called set character encoding to " + SecurityUtils.crlf(s) + " on a request without a content type. Character encoding will not be set");
            return;
        }
        putSingle(request.getMultiValueHeaders(), HttpHeaders.CONTENT_TYPE, appendCharacterEncoding(currentContentType, s));
    }

    @Override
    public int getContentLength() {
        return AwsHttpServletRequestHelper.getContentLength(request.getMultiValueHeaders());
    }


    @Override
    public long getContentLengthLong() {
        return AwsHttpServletRequestHelper.getContentLengthLong(request.getMultiValueHeaders());
    }


    @Override
    public String getContentType() {
        return AwsHttpServletRequestHelper.getContentType(request.getMultiValueHeaders());
    }

    @Override
    public String getParameter(String s) {
        return AwsHttpServletRequestHelper.getParameter(
                request.getMultiValueQueryStringParameters(),
                s,
                config,
                this
        );
    }


    @Override
    public Enumeration<String> getParameterNames() {
        return AwsHttpServletRequestHelper.getParameterNames(
                request.getMultiValueQueryStringParameters(),
                this
        );
    }


    @Override
    @SuppressFBWarnings("PZLA_PREFER_ZERO_LENGTH_ARRAYS") // suppressing this as according to the specs we should be returning null here if we can't find params
    public String[] getParameterValues(String s) {
        return AwsHttpServletRequestHelper.getParameterValues(
                request.getMultiValueQueryStringParameters(),
                s,
                config,
                this
        );
    }


    @Override
    public Map<String, String[]> getParameterMap() {
        return AwsHttpServletRequestHelper.getParameterMap(
                request.getMultiValueQueryStringParameters(),
                config,
                this
        );
    }


    @Override
    public String getProtocol() {
        return "";
    }


    @Override
    public String getScheme() {
        return AwsHttpServletRequestHelper.getScheme(
                request.getMultiValueHeaders(),
                this
        );
    }

    @Override
    public String getServerName() {
        return AwsHttpServletRequestHelper.getServerName(
                request.getMultiValueHeaders(),
                null
        );
    }

    @Override
    public int getServerPort() {
        return AwsHttpServletRequestHelper.getServerPort(request.getMultiValueHeaders());
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        return AwsHttpServletRequestHelper.getInputStream(
                requestInputStream,
                request.getBody(),
                request.getIsBase64Encoded(),
                this
        );
    }


    @Override
    public BufferedReader getReader()
            throws IOException {
        return AwsHttpServletRequestHelper.getReader(request.getBody());
    }


    @Override
    public String getRemoteAddr() {
        return "";
    }


    @Override
    public String getRemoteHost() {
        return AwsHttpServletRequestHelper.getRemoteHost(request.getMultiValueHeaders());
    }


    @Override
    public Locale getLocale() {
        return AwsHttpServletRequestHelper.getLocale(
                request.getMultiValueHeaders(),
                this
        );
    }

    @Override
    public Enumeration<Locale> getLocales() {
        return AwsHttpServletRequestHelper.getLocales(
                request.getMultiValueHeaders(),
                this
        );
    }

    @Override
    public boolean isSecure() {
        return AwsHttpServletRequestHelper.isSecure(securityContext);
    }



    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return AwsHttpServletRequestHelper.getRequestDispatcher(s, this);
    }


    @Override
    public int getRemotePort() {
        return 0;
    }


    @Override
    public boolean isAsyncSupported() {
        return true;
    }

    @Override
    public boolean isAsyncStarted() {
        if (asyncContext == null) {
            return false;
        }
        if (asyncContext.isCompleted() || asyncContext.isDispatched()) {
            return false;
        }
        return true;
    }


    @Override
    public AsyncContext startAsync()
            throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response, containerHandler);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(request.getRequestContext().getElb().getTargetGroupArn()));
        return asyncContext;
    }


    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse)
            throws IllegalStateException {
        return AwsHttpServletRequestHelper.startAsync(
                asyncContext,
                request.getRequestContext().getElb().getTargetGroupArn(),
                log,
                servletRequest,
                servletResponse,
                containerHandler
        );
    }

    @Override
    public AsyncContext getAsyncContext() {
        return AwsHttpServletRequestHelper.getAsyncContext(
                asyncContext,
                request.getRequestContext().getElb().getTargetGroupArn()
        );
    }

    @Override
    public String getRequestId() {
        return "";
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    //-------------------------------------------------------------
    // Methods - Private
    //-------------------------------------------------------------

    private List<String> getHeaderValues(String key) {
        if (Objects.isNull(request.getMultiValueHeaders())) {
            return null;
        }

        return request.getMultiValueHeaders().get(key);
    }
}
