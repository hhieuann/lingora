package com.lingora.dto;

import com.fasterxml.jackson.databind.JsonNode;

import java.time.Instant;

/** Buổi học đầy đủ (kèm segments + summary đã parse lại). */
public record StudySessionResponse(
        Long id,
        String title,
        String srcLang,
        String tgtLang,
        int durationSeconds,
        int segmentCount,
        int wordCount,
        Instant createdAt,
        JsonNode segments,
        JsonNode summary
) {}
