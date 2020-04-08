/*
 * Copyright 2020 Amazon.com, Inc. or its affiliates. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License"). You may not use this file except in compliance
 * with the License. A copy of the License is located at
 *
 * http://aws.amazon.com/apache2.0/
 *
 * or in the "license" file accompanying this file. This file is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES
 * OR CONDITIONS OF ANY KIND, either express or implied. See the License for the specific language governing permissions
 * and limitations under the License.
 */
package com.amazonaws.serverless.proxy.internal.servlet;

import com.amazonaws.serverless.proxy.LogFormatter;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;

import com.amazonaws.serverless.proxy.model.HttpApiV2ProxyRequestContext;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.ws.rs.core.SecurityContext;

import java.time.*;
import java.time.format.DateTimeFormatter;
import java.time.format.DateTimeFormatterBuilder;
import java.util.Locale;

import static com.amazonaws.serverless.proxy.RequestReader.*;
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
    private final DateTimeFormatter dateFormat;
    private final Clock clock;

    public ApacheCombinedServletLogFormatter() {
        this(Clock.systemDefaultZone());
    }

    ApacheCombinedServletLogFormatter(Clock clock) {
        this.clock = clock;
        this.dateFormat = new DateTimeFormatterBuilder()
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
        AwsProxyRequestContext gatewayContext = (AwsProxyRequestContext)servletRequest.getAttribute(API_GATEWAY_CONTEXT_PROPERTY);
        HttpApiV2ProxyRequestContext httpApiContext = (HttpApiV2ProxyRequestContext)servletRequest.getAttribute(HTTP_API_CONTEXT_PROPERTY);

        // %h
        logLineBuilder.append(servletRequest.getRemoteAddr());
        logLineBuilder.append(" ");

        // %l
        if (servletRequest.getUserPrincipal() != null) {
            logLineBuilder.append(servletRequest.getUserPrincipal().getName());
        } else {
            logLineBuilder.append("-");
        }
        if (gatewayContext != null && gatewayContext.getIdentity() != null && gatewayContext.getIdentity().getUserArn() != null) {
                logLineBuilder.append(gatewayContext.getIdentity().getUserArn());
        } else {
            logLineBuilder.append("-");
        }
        logLineBuilder.append(" ");

        // %u
        if (servletRequest.getUserPrincipal() != null) {
            logLineBuilder.append(servletRequest.getUserPrincipal().getName());
        }
        logLineBuilder.append(" ");


        // %t
        long timeEpoch = ZonedDateTime.now(clock).toEpochSecond();
        if (gatewayContext != null && gatewayContext.getRequestTimeEpoch() > 0) {
            timeEpoch = gatewayContext.getRequestTimeEpoch() / 1000;
        } else if (httpApiContext != null && httpApiContext.getTimeEpoch() > 0) {
            timeEpoch = httpApiContext.getTimeEpoch() / 1000;
        }
        logLineBuilder.append(
                dateFormat.format(ZonedDateTime.of(
                        LocalDateTime.ofEpochSecond(timeEpoch, 0, ZoneOffset.UTC),
                        clock.getZone())
                ));
        logLineBuilder.append(" ");

        // %r
        logLineBuilder.append("\"");
        logLineBuilder.append(servletRequest.getMethod().toUpperCase(Locale.ENGLISH));
        logLineBuilder.append(" ");
        logLineBuilder.append(servletRequest.getRequestURI());
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
