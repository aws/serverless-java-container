package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.LogFormatter;
import com.amazonaws.serverless.proxy.model.ApiGatewayRequestContext;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_CONTEXT_PROPERTY;
import static java.time.temporal.ChronoField.DAY_OF_MONTH;
import static java.time.temporal.ChronoField.HOUR_OF_DAY;
import static java.time.temporal.ChronoField.MINUTE_OF_HOUR;
import static java.time.temporal.ChronoField.MONTH_OF_YEAR;
import static java.time.temporal.ChronoField.SECOND_OF_MINUTE;
import static java.time.temporal.ChronoField.YEAR;


/**
 * Default implementation of the log formatter. Based on an <code>HttpServletRequest</code> and <code>HttpServletResponse</code> implementations produced
 * a log line in the Apache combined log format: https://httpd.apache.org/docs/2.4/logs.html
 * @param <ContainerRequestType> An implementation of <code>HttpServletRequest</code>
 * @param <ContainerResponseType> An implementation of <code>HttpServletResponse</code>
 */
public class ApacheCombinedServletLogFormatter<ContainerRequestType extends HttpServletRequest, ContainerResponseType extends HttpServletResponse>
        implements LogFormatter<ContainerRequestType, ContainerResponseType> {
    DateTimeFormatter dateFormat;

    public ApacheCombinedServletLogFormatter() {
        dateFormat = new DateTimeFormatterBuilder()
            .parseCaseInsensitive()
            .appendLiteral("[")
            .appendValue(DAY_OF_MONTH, 2)
            .appendLiteral("/")
            .appendValue(MONTH_OF_YEAR, 2)
            .appendLiteral("/")
            .appendValue(YEAR, 4)
            .appendLiteral(":")
            .appendValue(HOUR_OF_DAY, 2)
            .appendLiteral(":")
            .appendValue(MINUTE_OF_HOUR, 2)
            .appendLiteral(":")
            .appendValue(SECOND_OF_MINUTE, 2)
            .optionalStart()
            .appendOffset("+HHMM", "Z")
            .optionalEnd()
            .appendLiteral("]")
            .toFormatter();
    }

    @Override
    @SuppressFBWarnings({ "SERVLET_HEADER_REFERER", "SERVLET_HEADER_USER_AGENT" })
    public String format(ContainerRequestType servletRequest, ContainerResponseType servletResponse, SecurityContext ctx) {
        //LogFormat "%h %l %u %t \"%r\" %>s %b \"%{Referer}i\" \"%{User-agent}i\"" combined
        StringBuilder logLineBuilder = new StringBuilder();
        ApiGatewayRequestContext gatewayContext = (ApiGatewayRequestContext)servletRequest.getAttribute(API_GATEWAY_CONTEXT_PROPERTY);

        // %h
        logLineBuilder.append(servletRequest.getRemoteAddr());
        logLineBuilder.append(" ");

        // %l
        if (gatewayContext != null) {
            if (gatewayContext.getIdentity().getUserArn() != null) {
                logLineBuilder.append(gatewayContext.getIdentity().getUserArn());
            } else {
                logLineBuilder.append("-");
            }
        } else {
            logLineBuilder.append("-");
        }
        logLineBuilder.append(" ");

        // %u
        if (ctx != null && ctx.getUserPrincipal().getName() != null) {
            logLineBuilder.append(ctx.getUserPrincipal().getName());
            logLineBuilder.append(" ");
        }


        // %t
        if (gatewayContext != null) {
            logLineBuilder.append(
                    dateFormat.format(
                            ZonedDateTime.of(
                                    LocalDateTime.ofEpochSecond(gatewayContext.getRequestTimeEpoch() / 1000, 0, ZoneOffset.UTC),
                                    ZoneId.systemDefault()
                            )
                    )
            );
        } else {
            logLineBuilder.append(dateFormat.format(ZonedDateTime.now()));
        }
        logLineBuilder.append(" ");

        // %r
        logLineBuilder.append("\"");
        logLineBuilder.append(servletRequest.getMethod().toUpperCase(Locale.ENGLISH));
        logLineBuilder.append(" ");
        logLineBuilder.append(servletRequest.getPathInfo());
        logLineBuilder.append(" ");
        logLineBuilder.append(servletRequest.getProtocol());
        logLineBuilder.append("\" ");

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
        if (servletRequest.getHeader("referer") != null) {
            logLineBuilder.append(servletRequest.getHeader("referer"));
        } else {
            logLineBuilder.append("-");
        }
        logLineBuilder.append("\" ");

        // \"%{User-agent}i\"
        logLineBuilder.append("\"");
        if (servletRequest.getHeader("user-agent") != null) {
            logLineBuilder.append(servletRequest.getHeader("user-agent"));
        } else {
            logLineBuilder.append("-");
        }
        logLineBuilder.append("\" ");

        logLineBuilder.append("combined");


        return logLineBuilder.toString();
    }
}
