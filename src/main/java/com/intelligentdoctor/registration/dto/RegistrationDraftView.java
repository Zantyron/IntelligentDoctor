package com.intelligentdoctor.registration.dto;

import java.time.LocalDate;

public record RegistrationDraftView(
        String draftId,
        String sessionId,
        String symptomSummary,
        String departmentId,
        String clinicRoomId,
        String doctorId,
        String slotId,
        LocalDate visitDate,
        String visitPeriod,
        String patientName,
        String patientPhone,
        String idCard,
        String status
) {
}
