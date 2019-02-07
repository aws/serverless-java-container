package com.amazonaws.serverless.proxy.spring.echoapp.profile;

import com.amazonaws.serverless.proxy.spring.echoapp.model.MapResponseModel;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/profile")
public class DefaultProfileResource {
    @Value("${spring-proxy.profile-test}")
    private String profileTest;

    @Value("${spring-proxy.not-overridden-test}")
    private String noOverride;

    @Autowired
    private String beanInjectedValue;

    @RequestMapping(path = "/spring-properties", method = RequestMethod.GET)
    public MapResponseModel loadProperties() {
        MapResponseModel model = new MapResponseModel();

        model.addValue("profileTest", profileTest);
        model.addValue("noOverride", noOverride);
        model.addValue("beanInjectedValue", beanInjectedValue);
        return model;
    }
}