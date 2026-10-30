package com.lottery.ticket_service.controller;

import com.lottery.ticket_service.security.AuthenticatedUser;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WhoAmIController {

    @GetMapping("/api/whoami")
    public AuthenticatedUser whoAmI(@AuthenticationPrincipal AuthenticatedUser user) {
        return user;
    }
}