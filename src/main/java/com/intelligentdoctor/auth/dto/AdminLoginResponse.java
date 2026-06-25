package com.intelligentdoctor.auth.dto;

public record AdminLoginResponse(
        String token,
        String tokenType,
        long expiresAtEpochSeconds,
        String hospitalId,
        String username,
        String role
) {
}
