package com.lottery.ticket_service.security;

public record AuthenticatedUser(Long userId, String username) {}