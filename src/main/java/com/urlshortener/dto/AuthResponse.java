package com.urlshortener.dto;

import lombok.Getter;

@Getter
public class AuthResponse {
    private final String token;
    private final String type = "Bearer";

    public AuthResponse(String token) {
        this.token = token;
    }
}
