package com.intelligentdoctor.auth;

public record AdminPrincipal(
        String hospitalId,
        String username,
        String role
) {
}
