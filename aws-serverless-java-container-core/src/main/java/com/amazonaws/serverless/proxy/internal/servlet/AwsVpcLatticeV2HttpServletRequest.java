package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.internal.LambdaContainerHandler;
import com.amazonaws.serverless.proxy.internal.SecurityUtils;
import com.amazonaws.serverless.proxy.model.*;
import com.amazonaws.services.lambda.runtime.Context;
import jakarta.servlet.*;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.servlet.http.HttpUpgradeHandler;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.SecurityContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.StringReader;
import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.security.Principal;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Implementation of the <code>HttpServletRequest</code> interface that supports <code>VPCLatticeV2RequestEvent</code> object.
 * This object is initialized with an <code>VPCLatticeV2RequestEvent</code> event and a <code>SecurityContext</code> generated
 * by an implementation of the <code>SecurityContextWriter</code>.
 */
public class AwsVpcLatticeV2HttpServletRequest extends AwsHttpServletRequest {

    //-------------------------------------------------------------
    // Variables - Private
    //-------------------------------------------------------------


    private final VPCLatticeV2RequestEvent request;
    private final SecurityContext securityContext;
    private final MultiValuedTreeMap<String, String> queryString;
    private final Headers headers;
    private AwsAsyncContext asyncContext;
    private final Context lambdaContext;
    private static final Logger log = LoggerFactory.getLogger(AwsVpcLatticeV2HttpServletRequest.class);
    private final ContainerConfig config;

    //-------------------------------------------------------------
    // Constructors
    //-------------------------------------------------------------


    public AwsVpcLatticeV2HttpServletRequest(VPCLatticeV2RequestEvent vpcLatticeV2Request, Context lambdaContext, SecurityContext awsSecurityContext) {
        this(vpcLatticeV2Request, lambdaContext, awsSecurityContext, LambdaContainerHandler.getContainerConfig());
    }


    public AwsVpcLatticeV2HttpServletRequest(VPCLatticeV2RequestEvent vpcLatticeV2Request, Context lambdaContext, SecurityContext awsSecurityContext, ContainerConfig config) {
        super(lambdaContext);
        this.request = vpcLatticeV2Request;
        this.securityContext = awsSecurityContext;
        this.lambdaContext = lambdaContext;
        this.config = config;
        headers = request.getHeaders();
        queryString = queryStringToMultiValue(request.getQueryStringParameters());
    }

    @Override
    public String getAuthType() {
        return securityContext.getAuthenticationScheme();
    }

    @Override
    public Cookie[] getCookies() {
        if (headers == null || !headers.containsKey(HttpHeaders.COOKIE)) {
            return new Cookie[0];
        } else {
            return parseCookieHeaderValue(headers.getFirst(HttpHeaders.COOKIE));
        }
    }

    @Override
    public long getDateHeader(String s) {
        return getDateHeader(s, headers);
    }

    @Override
    public String getHeader(String s) {
        return getHeader(s, headers);
    }

    @Override
    public Enumeration<String> getHeaders(String s) {
        return getHeaders(s, headers);
    }

    @Override
    public Enumeration<String> getHeaderNames() {
        return getHeaderNames(headers);
    }

    @Override
    public int getIntHeader(String s) {
        return getIntHeader(s, headers);
    }

    @Override
    public String getMethod() {
        return request.getMethod();
    }

    @Override
    public String getPathInfo() {
        String pathInfo = cleanUri(request.getPath());
        return decodeRequestPath(pathInfo, LambdaContainerHandler.getContainerConfig());
    }

    @Override
    public String getPathTranslated() {
        return null;
    }

    @Override
    public String getContextPath() {
        return generateContextPath(config, null);
    }

    @Override
    public String getQueryString() {

        if (Objects.isNull(queryString))
            return null;

        try {
            StringBuilder queryStringBuilder = new StringBuilder();

            try {
                for (String key : queryString.keySet()) {
                    String val = queryString.getFirst(key);
                    queryStringBuilder.append("&");
                    queryStringBuilder.append(URLEncoder.encode(key, config.getUriEncoding()));
                    queryStringBuilder.append("=");
                    if (val != null) {
                        queryStringBuilder.append(URLEncoder.encode(val, config.getUriEncoding()));
                    }
                }
            } catch (UnsupportedEncodingException e) {
                throw new ServletException("Invalid charset passed for query string encoding", e);
            }

            return queryStringBuilder.substring(1); // remove the first & - faster to do it here than adding logic in the Lambda

        } catch (ServletException e) {
            log.error("Could not generate query string", e);
            return null;
        }
    }

    @Override
    public String getRemoteUser() {
        if (securityContext == null || securityContext.getUserPrincipal() == null) {
            return null;
        }
        return securityContext.getUserPrincipal().getName();
    }

    @Override
    public boolean isUserInRole(String s) {
        return false;
    }

    @Override
    public Principal getUserPrincipal() {
        if (securityContext == null) {
            return null;
        }
        return securityContext.getUserPrincipal();
    }

    @Override
    public String getRequestURI() {
        return cleanUri(getContextPath()) + cleanUri(request.getPath());
    }

    @Override
    public StringBuffer getRequestURL() {
        return generateRequestURL(request.getPath());
    }

    @Override
    public boolean authenticate(HttpServletResponse httpServletResponse) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void login(String s, String s1) throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public void logout() throws ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public <T extends HttpUpgradeHandler> T upgrade(Class<T> aClass) throws IOException, ServletException {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getCharacterEncoding() {
        if (headers == null) {
            return config.getDefaultContentCharset();
        }
        return parseCharacterEncoding(headers.getFirst(HttpHeaders.CONTENT_TYPE));
    }

    @Override
    public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
        setCharacterEncoding(s, headers);
    }

    @Override
    public int getContentLength() {
        return getContentLength(headers);
    }

    @Override
    public long getContentLengthLong() {
        return getContentLengthLong(headers);
    }

    @Override
    public String getContentType() {
        return headers.getFirst(HttpHeaders.CONTENT_TYPE);
    }

    @Override
    public ServletInputStream getInputStream() throws IOException {
        if (requestInputStream == null) {
            requestInputStream = new AwsServletInputStream(bodyStringToInputStream(request.getBody(), Boolean.TRUE.equals(request.getIsBase64Encoded())));
        }
        return requestInputStream;
    }

    @Override
    public String getParameter(String s) {
        return getParameter(queryString, s, config.isQueryStringCaseSensitive());
    }

    @Override
    public Enumeration<String> getParameterNames() {
        Set<String> formParameterNames = getFormUrlEncodedParametersMap().keySet();
        if (queryString == null) {
            return Collections.enumeration(formParameterNames);
        }
        return Collections.enumeration(Stream.concat(formParameterNames.stream(),
                queryString.keySet().stream()).collect(Collectors.toSet()));
    }

    @Override
    public String[] getParameterValues(String s) {
        return getParameterValues(queryString, s, config.isQueryStringCaseSensitive());
    }

    @Override
    public Map<String, String[]> getParameterMap() {
        return generateParameterMap(queryString, config);
    }

    @Override
    public String getProtocol() {
        // No protocol on the request payload. Defaulting to "HTTP/1.1". Should we return UnsupportedOperationException instead?
        return "HTTP/1.1";
    }

    @Override
    public String getScheme() {
        return getSchemeFromHeader(request.getHeaders());
    }

    @Override
    public BufferedReader getReader() throws IOException {
        return new BufferedReader(new StringReader(request.getBody()));
    }

    @Override
    public String getRemoteAddr() {
        return request.getHeaders().getFirst(CLIENT_IP_HEADER_NAME);
    }

    @Override
    public String getRemoteHost() {
        return request.getHeaders().getFirst(HttpHeaders.HOST);
    }

    @Override
    public Locale getLocale() {
        List<Locale> locales = parseAcceptLanguageHeader(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));
        return locales.isEmpty() ? Locale.getDefault() : locales.get(0);
    }

    @Override
    public Enumeration<Locale> getLocales() {
        List<Locale> locales = parseAcceptLanguageHeader(headers.getFirst(HttpHeaders.ACCEPT_LANGUAGE));
        return Collections.enumeration(locales);
    }

    @Override
    public boolean isSecure() {
        return securityContext.isSecure();
    }

    @Override
    public RequestDispatcher getRequestDispatcher(String s) {
        return getServletContext().getRequestDispatcher(s);
    }

    @Override
    public int getRemotePort() {
        return 0;
    }

    @Override
    public AsyncContext startAsync() throws IllegalStateException {
        asyncContext = new AwsAsyncContext(this, response);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(lambdaContext.getAwsRequestId()));
        return asyncContext;
    }

    @Override
    public AsyncContext startAsync(ServletRequest servletRequest, ServletResponse servletResponse) throws IllegalStateException {
        asyncContext = new AwsAsyncContext((HttpServletRequest) servletRequest, (HttpServletResponse) servletResponse);
        setAttribute(DISPATCHER_TYPE_ATTRIBUTE, DispatcherType.ASYNC);
        log.debug("Starting async context for request: " + SecurityUtils.crlf(lambdaContext.getAwsRequestId()));
        return asyncContext;
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
        return !asyncContext.isCompleted() && !asyncContext.isDispatched();
    }

    @Override
    public AsyncContext getAsyncContext() {
        if (asyncContext == null) {
            throw new IllegalStateException("Request " + SecurityUtils.crlf(lambdaContext.getAwsRequestId())
                    + " is not in asynchronous mode. Call startAsync before attempting to get the async context.");
        }
        return asyncContext;
    }

    @Override
    public String getRequestId() {
        return lambdaContext.getAwsRequestId();
    }

    @Override
    public String getProtocolRequestId() {
        return "";
    }

    @Override
    public ServletConnection getServletConnection() {
        return null;
    }

    @Override
    public int getServerPort() {
        if (request.getHeaders() == null) {
            return 443;
        }
        String port = request.getHeaders().getFirst(PORT_HEADER_NAME);
        if (SecurityUtils.isValidPort(port)) {
            return Integer.parseInt(port);
        } else {
            return 443; // default port
        }
    }

    @Override
    public String getServerName() {
        return request.getHeaders().getFirst(HOST_HEADER_NAME);
    }

    protected static MultiValuedTreeMap<String, String> queryStringToMultiValue(Map<String, String> qs) {
        if (qs == null || qs.isEmpty()) {
            return null;
        }
        MultiValuedTreeMap<String, String> qsMap = new MultiValuedTreeMap<>();
        for (Map.Entry<String, String> kv : qs.entrySet()) {
            qsMap.add(kv.getKey(), kv.getValue());
        }
        return qsMap;
    }

}
