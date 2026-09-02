package com.billim.api.auth;

public record SignupRequest(String email, String password, String name) {
}