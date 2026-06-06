package com.intelligentdoctor.registration.dto;

import java.time.LocalDate;
import java.time.LocalDateTime;

public record RegistrationOrderView(
        String id,
        String orderNo,
        String sessionId,
        String patientName,
        String patientPhone,
        String departmentId,
        String clinicRoomId,
        String doctorId,
        String slotId,
        LocalDate visitDate,
        String visitPeriod,
        String status,
        String symptomSummary,
        LocalDateTime createdAt
) {
}
