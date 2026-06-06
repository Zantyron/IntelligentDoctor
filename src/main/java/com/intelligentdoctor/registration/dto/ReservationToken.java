package com.intelligentdoctor.registration.dto;

public record ReservationToken(
        boolean success,
        String token,
        String message
) {
}
