package com.intelligentdoctor.ai.dto;

import java.util.List;
import java.util.Map;

public record TriageAnalysis(
        String symptomSummary,
        List<String> possibleConditions,
        String urgencyLevel,
        List<String> suggestedDepartments,
        List<String> cautionNotes,
        Map<String, String> extractedSlots
) {
}
