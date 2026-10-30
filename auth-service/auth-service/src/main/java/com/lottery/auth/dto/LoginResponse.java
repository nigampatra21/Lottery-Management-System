package com.lottery.auth.dto;

public class LoginResponse {
    private Long userId;
    private String username;
    private String accessToken;
    private String tokenType;

    public LoginResponse() {
    }

    public LoginResponse(Long userId, String username, String accessToken, String tokenType) {
        this.userId = userId;
        this.username = username;
        this.accessToken = accessToken;
        this.tokenType = tokenType;
    }

    public Long getUserId() {
        return userId;
    }

    public String getUsername() {
        return username;
    }

    public String getAccessToken() {
        return accessToken;
    }

    public String getTokenType() {
        return tokenType;
    }
}
