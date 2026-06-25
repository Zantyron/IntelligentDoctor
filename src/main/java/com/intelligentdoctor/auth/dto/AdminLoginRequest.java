package com.intelligentdoctor.auth.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminLoginRequest(
        @NotBlank(message = "username 不能为空")
        String username,
        @NotBlank(message = "password 不能为空")
        String password
) {
}
