package com.intelligentdoctor.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

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
        String idCard,
        @NotBlank(message = "gender 不能为空")
        String gender,
        @NotNull(message = "age 不能为空")
        @Min(value = 0, message = "age 不能小于 0")
        @Max(value = 130, message = "age 不能大于 130")
        Integer age
) {
}
