package com.intelligentdoctor.catalog.dto;

public record DoctorView(
        String id,
        String name,
        String title,
        String specialty,
        String introduction,
        boolean hotExpert,
        int consultationFee
) {
}
