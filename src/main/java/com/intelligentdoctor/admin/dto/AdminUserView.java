package com.intelligentdoctor.admin.dto;

import java.time.LocalDateTime;

public record AdminUserView(
        String id,
        String username,
        Boolean enabled,
        boolean currentUser,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
