package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.LogFormatter;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Locale;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_CONTEXT_PROPERTY;


/**
 * Default implementation of the log formatter. Based on an <code>HttpServletRequest</code> and <code>HttpServletResponse</code> implementations produced
 * a log line in the Apache combined log format: https://httpd.apache.org/docs/2.4/logs.html
 * @param <ContainerRequestType> An implementation of <code>HttpServletRequest</code>
 * @param <ContainerResponseType> An implementation of <code>HttpServletResponse</code>
 */
public class ApacheCombinedServletLogFormatter<ContainerRequestType extends HttpServletRequest, ContainerResponseType extends HttpServletResponse>
        implements LogFormatter<ContainerRequestType, ContainerResponseType> {
    SimpleDateFormat dateFormat;

    public ApacheCombinedServletLogFormatter() {
        dateFormat = new SimpleDateFormat("[dd/MM/yyyy:hh:mm:ss Z]");
    }

    @Override
    @SuppressFBWarnings({ "SERVLET_HEADER_REFERER", "SERVLET_HEADER_USER_AGENT" })
    public String format(ContainerRequestType servletRequest, ContainerResponseType servletResponse, SecurityContext ctx) {
        //LogFormat "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-agent}i\"" combined
        StringBuilder logLineBuilder = new StringBuilder();

        // %h
        logLineBuilder.append(servletRequest.getRemoteAddr());
        logLineBuilder.append(" ");

        // %l
        if (servletRequest instanceof AwsProxyHttpServletRequest && servletRequest.getAttribute(API_GATEWAY_CONTEXT_PROPERTY) != null) {
            ApiGatewayRequestContext gatewayContext = (ApiGatewayRequestContext)servletRequest.getAttribute(API_GATEWAY_CONTEXT_PROPERTY);
            logLineBuilder.append(gatewayContext.getIdentity().getUserArn());
            logLineBuilder.append(" ");
        } else {
            logLineBuilder.append("- ");
        }

        // %u
        if (ctx != null) {
            logLineBuilder.append(ctx.getUserPrincipal().getName());
        }
        logLineBuilder.append(" ");


        // %t
        logLineBuilder.append(dateFormat.format(Calendar.getInstance().getTime()));
        logLineBuilder.append(" ");

        // %r
        logLineBuilder.append("\"");
        logLineBuilder.append(servletRequest.getMethod().toUpperCase(Locale.ENGLISH));
        logLineBuilder.append(" ");
        logLineBuilder.append(servletRequest.getPathInfo());
        logLineBuilder.append(" ");
        logLineBuilder.append(servletRequest.getProtocol());
        logLineBuilder.append(" \" ");

        // %>s
        logLineBuilder.append(servletResponse.getStatus());
        logLineBuilder.append(" ");

        // %b
        if (servletResponse instanceof AwsHttpServletResponse) {
            AwsHttpServletResponse awsResponse = (AwsHttpServletResponse)servletResponse;
            if (awsResponse.getAwsResponseBodyBytes().length > 0) {
                logLineBuilder.append(awsResponse.getAwsResponseBodyBytes().length);
            } else {
                logLineBuilder.append("-");
            }
        } else {
            logLineBuilder.append("-");
        }
        logLineBuilder.append(" ");

        // \"%{Referer}i\"
        logLineBuilder.append("\"");
        logLineBuilder.append(servletRequest.getHeader("referer"));
        logLineBuilder.append("\"");

        // \"%{User-agent}i\"
        logLineBuilder.append("\"");
        logLineBuilder.append(servletRequest.getHeader("user-agent"));
        logLineBuilder.append("\"");

        logLineBuilder.append(" combined");


        return logLineBuilder.toString();
    }
}
