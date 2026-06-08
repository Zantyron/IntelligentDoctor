package com.intelligentdoctor.registration.dto;

import java.time.LocalDate;

public record CreateDraftCommand(
        String hospitalId,
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
        String gender,
        Integer age,
        String patientId
) {
}
