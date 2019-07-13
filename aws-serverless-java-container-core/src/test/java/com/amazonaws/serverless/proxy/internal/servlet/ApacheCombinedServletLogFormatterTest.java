package com.amazonaws.serverless.proxy.internal.servlet;


import com.amazonaws.serverless.proxy.model.ApiGatewayRequestIdentity;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyRequestContext;
import org.junit.Before;
import org.junit.Test;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;

import static com.amazonaws.serverless.proxy.RequestReader.API_GATEWAY_EVENT_PROPERTY;
import static org.hamcrest.CoreMatchers.containsString;
import static org.junit.Assert.assertThat;
import static org.mockito.Matchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class ApacheCombinedServletLogFormatterTest {

  private ApacheCombinedServletLogFormatter sut;

  private HttpServletRequest mockServletRequest;
  private HttpServletResponse mockServletResponse;
  private AwsProxyRequest proxyRequest;
  private AwsProxyRequestContext context;

  @Before
  public void setup() {
    proxyRequest = new AwsProxyRequest();
    Clock fixedClock = Clock.fixed(Instant.ofEpochSecond(665888523L), ZoneId.of("UTC"));
    mockServletRequest = mock(HttpServletRequest.class);
    when(mockServletRequest.getAttribute(eq(API_GATEWAY_EVENT_PROPERTY)))
        .thenReturn(proxyRequest);
    when(mockServletRequest.getMethod())
        .thenReturn("GET");
    mockServletResponse = mock(HttpServletResponse.class);
    context = new AwsProxyRequestContext();
    context.setIdentity(new ApiGatewayRequestIdentity());
    proxyRequest.setRequestContext(context);

    sut = new ApacheCombinedServletLogFormatter(fixedClock);
  }

  @Test
  public void logsCurrentTimeWhenContextNull() {
    // given
    proxyRequest.setRequestContext(null);

    // when
    String actual = sut.format(mockServletRequest, mockServletResponse, null);

    // then
    assertThat(actual, containsString("[07/02/1991:01:02:03Z]"));
  }

  @Test
  public void logsCurrentTimeWhenRequestTimeZero() {
    // given
    context.setRequestTimeEpoch(0);

    // when
    String actual = sut.format(mockServletRequest, mockServletResponse, null);

    // then
    assertThat(actual, containsString("[07/02/1991:01:02:03Z]"));
  }

  @Test
  public void logsRequestTimeWhenRequestTimeEpochGreaterThanZero() {
    // given
    context.setRequestTimeEpoch(1563023494000L);

    // when
    String actual = sut.format(mockServletRequest, mockServletResponse, null);

    // then
    assertThat(actual, containsString("[13/07/2019:13:11:34Z]"));
  }

}
