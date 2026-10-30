package com.lottery.draw.security;

public record AuthenticatedUser(Long userId, String username) {}