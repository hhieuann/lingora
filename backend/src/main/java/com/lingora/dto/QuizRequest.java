package com.lingora.dto;

import jakarta.validation.constraints.NotBlank;
import java.util.List;

/** Yêu cầu tạo quiz: {"targetLang","count":5,"segments":[{"speaker","src","tgt"}]}. */
public record QuizRequest(
        @NotBlank String targetLang,
        Integer count,
        List<SummarizeRequest.Segment> segments
) {}
