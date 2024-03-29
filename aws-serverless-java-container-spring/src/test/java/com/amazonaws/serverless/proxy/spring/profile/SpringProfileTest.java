package com.amazonaws.serverless.proxy.spring.profile;

import com.amazonaws.serverless.proxy.model.AwsProxyRequest;
import com.amazonaws.serverless.proxy.model.AwsProxyResponse;
import com.amazonaws.serverless.proxy.internal.servlet.AwsServletContext;
import com.amazonaws.serverless.proxy.internal.testutils.AwsProxyRequestBuilder;
import com.amazonaws.serverless.proxy.internal.testutils.MockLambdaContext;
import com.amazonaws.serverless.proxy.spring.SpringLambdaContainerHandler;
import com.amazonaws.serverless.proxy.spring.echoapp.EchoSpringAppConfig;
import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestExecutionListeners;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.context.support.DependencyInjectionTestExecutionListener;
import org.springframework.test.context.web.WebAppConfiguration;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = EchoSpringAppConfig.class)
@WebAppConfiguration
@TestExecutionListeners(inheritListeners = false, listeners = {DependencyInjectionTestExecutionListener.class})
public class SpringProfileTest {
    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private MockLambdaContext lambdaContext;

    @BeforeEach
    public void clearServletContextCache() {
        AwsServletContext.clearServletContextCache();
    }

    @Test
    void profile_defaultProfile() throws Exception {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/profile/spring-properties", "GET")
                .build();

        SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
        assertEquals(3, response.getValues().size());
        assertEquals("default-profile", response.getValues().get("profileTest"));
        assertEquals("not-overridden", response.getValues().get("noOverride"));
        assertEquals("default-profile-from-bean", response.getValues().get("beanInjectedValue"));
    }

    @Test
    void profile_overrideProfile() throws Exception {
        AwsProxyRequest request = new AwsProxyRequestBuilder("/profile/spring-properties", "GET")
                .build();
        SpringLambdaContainerHandler<AwsProxyRequest, AwsProxyResponse> handler = SpringLambdaContainerHandler.getAwsProxyHandler(EchoSpringAppConfig.class);
        handler.activateSpringProfiles("override");
        AwsProxyResponse output = handler.proxy(request, lambdaContext);
        assertEquals(200, output.getStatusCode());

        MapResponseModel response = objectMapper.readValue(output.getBody(), MapResponseModel.class);
        assertEquals(3, response.getValues().size());
        assertEquals("override-profile", response.getValues().get("profileTest"));
        assertEquals("not-overridden", response.getValues().get("noOverride"));
        assertEquals("override-profile-from-bean", response.getValues().get("beanInjectedValue"));
    }
}