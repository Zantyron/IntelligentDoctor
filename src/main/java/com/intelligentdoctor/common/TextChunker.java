package com.intelligentdoctor.common;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
public class TextChunker {

    public List<String> chunk(String text, int maxLength, int overlap) {
        List<String> result = new ArrayList<>();
        if (text == null || text.isBlank()) {
            return result;
        }

        String normalized = text.replace("\r", "")
                .replace("\t", " ")
                .replaceAll("[ ]{2,}", " ")
                .trim();
        if (normalized.length() <= maxLength) {
            result.add(normalized);
            return result;
        }

        int start = 0;
        while (start < normalized.length()) {
            int end = Math.min(normalized.length(), start + maxLength);
            if (end < normalized.length()) {
                int sentenceBoundary = Math.max(
                        normalized.lastIndexOf('。', end),
                        Math.max(normalized.lastIndexOf('\n', end), normalized.lastIndexOf('；', end))
                );
                if (sentenceBoundary > start + maxLength / 2) {
                    end = sentenceBoundary + 1;
                }
            }
            result.add(normalized.substring(start, end).trim());
            if (end == normalized.length()) {
                break;
            }
            start = Math.max(end - overlap, start + 1);
        }
        return result;
    }
}
