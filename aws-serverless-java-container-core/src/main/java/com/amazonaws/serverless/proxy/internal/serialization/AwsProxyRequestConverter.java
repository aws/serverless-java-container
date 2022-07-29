package com.amazonaws.serverless.proxy.internal.serialization;

import java.util.Map;
import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.Headers;
import com.amazonaws.serverless.proxy.model.MultiValuedTreeMap;
import com.fasterxml.jackson.databind.JavaType;
import com.fasterxml.jackson.databind.type.TypeFactory;
import com.fasterxml.jackson.databind.util.Converter;

public class AwsProxyRequestConverter implements Converter<AwsProxyRequest, AwsProxyRequest> {
  @Override
  public AwsProxyRequest convert(AwsProxyRequest value) {
    if (value == null)
      return null;

    if ((value.getMultiValueHeaders() == null || value.getMultiValueHeaders().isEmpty())
        && value.getHeaders() != null && !value.getHeaders().isEmpty()) {
      Headers multiValueHeaders = new Headers();
      for (Map.Entry<String, String> e : value.getHeaders().entrySet())
        multiValueHeaders.putSingle(e.getKey(), e.getValue());
      value.setMultiValueHeaders(multiValueHeaders);
    }

    if ((value.getMultiValueQueryStringParameters() == null
        || value.getMultiValueQueryStringParameters().isEmpty())
        && value.getQueryStringParameters() != null
        && !value.getQueryStringParameters().isEmpty()) {
      MultiValuedTreeMap<String, String> multiValueQueryStringParameters =
          new MultiValuedTreeMap<>();
      for (Map.Entry<String, String> e : value.getQueryStringParameters().entrySet())
        multiValueQueryStringParameters.putSingle(e.getKey(), e.getValue());
      value.setMultiValueQueryStringParameters(multiValueQueryStringParameters);
    }
    
    return value;
  }

  @Override
  public JavaType getInputType(TypeFactory typeFactory) {
    return typeFactory.constructType(AwsProxyRequest.class);
  }

  @Override
  public JavaType getOutputType(TypeFactory typeFactory) {
    return typeFactory.constructType(AwsProxyRequest.class);
  }
}
