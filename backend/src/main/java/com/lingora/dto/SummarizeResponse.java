package com.lingora.dto;

import java.util.List;

/**
 * Phản hồi tóm tắt — KHỚP với renderSummary() ở frontend:
 * {"points":[...], "vocab":[{"term","lang","mean"}], "homework":[{"who","text"}]}.
 */
public record SummarizeResponse(
        List<String> points,
        List<Vocab> vocab,
        List<Homework> homework
) {
    public record Vocab(String term, String lang, String mean) {}
    public record Homework(String who, String text) {}
}
