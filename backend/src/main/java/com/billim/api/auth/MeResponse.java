package com.billim.api.auth;

public record MeResponse(Long userId, String email, String name, String role) {
}