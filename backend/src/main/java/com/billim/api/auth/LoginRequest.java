package com.billim.api.auth;

public record LoginRequest(String email, String password) {
}