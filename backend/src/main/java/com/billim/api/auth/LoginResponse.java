package com.billim.api.auth;

public record LoginResponse(String accessToken, Long userId, String name) {
}