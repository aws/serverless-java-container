package com.example.demo;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HelloController {
	
	public HelloController() {
		System.out.println("Creating controller");
	}

    @GetMapping("/hello")
    public String something(){
        return "Hello World";
    }
}
