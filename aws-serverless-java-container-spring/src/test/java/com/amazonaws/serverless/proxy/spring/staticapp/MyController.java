package com.amazonaws.serverless.proxy.spring.staticapp;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;


@EnableWebMvc
@Controller
public class MyController {
	@RequestMapping({ "/sample/page" })
	public String showPage() {
		return "sample";
	}
}
