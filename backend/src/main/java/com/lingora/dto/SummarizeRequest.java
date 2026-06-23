package com.lingora.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/**
 * Yêu cầu tóm tắt: {"targetLang", "segments":[{"speaker","src","tgt"}]}.
 * Gửi từ buildSummary() ở frontend.
 */
public record SummarizeRequest(
        @NotBlank String targetLang,
        List<Segment> segments
) {
    public record Segment(String speaker, String src, String tgt) {}
}
