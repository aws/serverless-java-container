package com.amazonaws.serverless.proxy.spring.jpaapp;

import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.context.request.async.DeferredResult;
import java.util.Collections;
import java.util.Map;

@RestController
public class MessageController {

	public static final String HELLO_MESSAGE = "Hello";

	@RequestMapping(path="/hello", method=RequestMethod.GET, produces = {"text/plain"})
	public String hello() {
		return HELLO_MESSAGE;
	}

    @SuppressWarnings({ "unchecked", "rawtypes" })
	@RequestMapping(path = "/async", method = RequestMethod.POST)
	@ResponseBody
	public DeferredResult<Map<String, String>> asyncResult(@RequestBody Map<String, String> value) {
		DeferredResult result = new DeferredResult<>();
		result.setResult(Collections.singletonMap("name", value.get("name").toUpperCase()));
		return result;
	}

}
