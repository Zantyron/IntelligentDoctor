package com.intelligentdoctor.admin.dto;

public record ImportResultSummary(
        int chunks,
        int hospitals,
        int departments,
        int clinics,
        int doctors,
        int schedules,
        int rules,
        String vectorProvider
) {
}
