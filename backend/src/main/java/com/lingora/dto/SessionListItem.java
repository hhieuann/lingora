package com.lingora.dto;

import java.time.Instant;

/** Mục trong danh sách buổi học (không kèm nội dung nặng). */
public record SessionListItem(
        Long id,
        String title,
        String srcLang,
        String tgtLang,
        int durationSeconds,
        int segmentCount,
        int wordCount,
        Instant createdAt
) {}
