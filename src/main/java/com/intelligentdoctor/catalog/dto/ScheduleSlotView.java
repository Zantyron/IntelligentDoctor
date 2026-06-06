package com.intelligentdoctor.catalog.dto;

import java.time.LocalDate;

public record ScheduleSlotView(
        String id,
        String doctorId,
        LocalDate slotDate,
        String period,
        int stockAvailable,
        boolean hotSlot
) {
}
