package com.intelligentdoctor.admin.dto;

import jakarta.validation.constraints.NotNull;

public record UpdateAdminUserRequest(
        @NotNull(message = "enabled 不能为空")
        Boolean enabled
) {
}
