package com.intelligentdoctor.registration.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record ConfirmRegistrationRequest(
        @NotBlank(message = "draftId 不能为空")
        @Size(max = 64, message = "draftId 不能超过 64 个字符")
        String draftId,
        @NotBlank(message = "sessionId 不能为空")
        @Size(max = 128, message = "sessionId 不能超过 128 个字符")
        String sessionId,
        @Size(max = 128, message = "idempotencyKey 不能超过 128 个字符")
        String idempotencyKey,
        @NotBlank(message = "patientName 不能为空")
        @Size(max = 64, message = "patientName 不能超过 64 个字符")
        String patientName,
        @NotBlank(message = "patientPhone 不能为空")
        @Pattern(regexp = "^1[3-9]\\d{9}$", message = "patientPhone 格式不正确")
        String patientPhone,
        @NotBlank(message = "idCard 不能为空")
        @Pattern(regexp = "^[0-9A-Za-z]{6,32}$", message = "idCard 格式不正确")
        String idCard,
        @NotBlank(message = "gender 不能为空")
        @Size(max = 16, message = "gender 不能超过 16 个字符")
        String gender,
        @NotNull(message = "age 不能为空")
        @Min(value = 0, message = "age 不能小于 0")
        @Max(value = 130, message = "age 不能大于 130")
        Integer age
) {
}
