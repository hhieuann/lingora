package com.lingora.dto;

import com.fasterxml.jackson.databind.JsonNode;
import jakarta.validation.constraints.NotBlank;

/**
 * Lưu buổi học. segments/summary nhận nguyên JSON từ frontend (giữ linh hoạt).
 * {"title","srcLang","tgtLang","durationSeconds","segments":[...],"summary":{...}}
 */
public record SaveSessionRequest(
        @NotBlank String title,
        String srcLang,
        String tgtLang,
        int durationSeconds,
        JsonNode segments,
        JsonNode summary
) {}
