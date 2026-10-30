
package com.lottery.auth.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/test")
public class TestController {

    @GetMapping("/protected")
    public Map<String, String> protectedEndpoint() {

        return Map.of(
                "message", "JWT authentication successful!",
                "status", "You accessed a protected endpoint."
        );
    }
}
