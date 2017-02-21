package com.amazonaws.serverless.proxy.spring.profile;

import com.amazonaws.serverless.proxy.internal.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.internal.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.web.WebAppConfiguration;

import java.io.IOException;

import static org.junit.Assert.assertEquals;


@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(classes = {EchoSpringAppConfig.class})
@WebAppConfiguration
public class SpringProfileTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockLambdaContext lambdaContext;

    @Autowired
    private SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler;

    @Test
    public void profile_defaultProfile() throws IOException {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/profile/spring-properties", "GET")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
        assertEquals("default-profile", response.getValues().get("profileTest"));
        assertEquals("not-overridden", response.getValues().get("noOverride"));
    }

    @Test
    public void profile_overrideProfile() throws IOException {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/profile/spring-properties", "GET")
                .stage("override")
                .build();

        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
        assertEquals("override-profile", response.getValues().get("profileTest"));
        assertEquals("not-overridden", response.getValues().get("noOverride"));
    }
}
