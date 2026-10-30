package com.lottery.audit.security;

public record AuthenticatedUser(Long userId, String username) {}