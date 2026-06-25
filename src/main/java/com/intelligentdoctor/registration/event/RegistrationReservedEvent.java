package com.intelligentdoctor.registration.event;

public record RegistrationReservedEvent(
        String token,
        String hospitalId,
        String draftId,
        String slotId,
        String sessionId
) {
}
