package com.intelligentdoctor.registration.dto;

import jakarta.validation.constraints.NotBlank;

public record ConfirmRegistrationRequest(
        @NotBlank(message = "draftId 不能为空")
        String draftId,
        @NotBlank(message = "sessionId 不能为空")
        String sessionId,
        String idempotencyKey,
        @NotBlank(message = "patientName 不能为空")
        String patientName,
        @NotBlank(message = "patientPhone 不能为空")
        String patientPhone,
        @NotBlank(message = "idCard 不能为空")
        String idCard
) {
}
