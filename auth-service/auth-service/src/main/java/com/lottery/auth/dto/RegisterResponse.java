package com.lottery.auth.dto;

import java.time.LocalDateTime;
public class RegisterResponse {
    private Long id;
    private String username;
    private String email;
    private String firstName;
    private String lastName;
    private String status;
    private LocalDateTime createdAt;

    public RegisterResponse() {
    }

    public RegisterResponse(
            Long id,
            String username,
            String email,
            String firstName,
            String lastName,
            String status,
            LocalDateTime createdAt) {

        this.id = id;
        this.username = username;
        this.email = email;
        this.firstName = firstName;
        this.lastName = lastName;
        this.status = status;
        this.createdAt = createdAt;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }

    public String getEmail() {
        return email;
    }

    public String getFirstName() {
        return firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public String getStatus() {
        return status;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

}
