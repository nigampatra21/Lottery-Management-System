package com.lottery.winner.security;

public record AuthenticatedUser(Long userId, String username) {}