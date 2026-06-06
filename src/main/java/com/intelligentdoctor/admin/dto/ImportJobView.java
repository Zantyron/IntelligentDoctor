package com.intelligentdoctor.admin.dto;

public record ImportJobView(
        String id,
        String fileName,
        String fileType,
        String status,
        Integer retryCount,
        String summaryJson,
        String errorMessage
) {
}
