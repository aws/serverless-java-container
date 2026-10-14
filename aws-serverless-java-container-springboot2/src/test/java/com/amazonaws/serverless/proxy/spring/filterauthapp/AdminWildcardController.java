package com.amazonaws.serverless.proxy.spring.filterauthapp;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

/** Mirrors the reviewer's scenario: an admin handler whose pattern can match a raw, un-normalized path. */
@RestController
public class AdminWildcardController {
    public static final String WILDCARD_SECRET = "WILDCARD_TOP_SECRET";

    @GetMapping("/admin/**")
    public String anyAdmin() {
        return WILDCARD_SECRET;
    }
}
