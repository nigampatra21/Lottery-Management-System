package com.lottery.draw.controller;

import com.lottery.draw.security.AuthenticatedUser;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/draws")
public class WhoAmIController {

    @GetMapping("/whoami")
    public AuthenticatedUser whoAmI(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }

    @PostMapping("/ping")
    @ResponseStatus(HttpStatus.CREATED)
    public Map<String, String> ping() {
        return Map.of("message", "admin only");
    }
}