package com.lingora.dto;

import jakarta.validation.constraints.NotBlank;

/** Yêu cầu dịch: {"text","source","target"}. */
public record TranslateRequest(
        @NotBlank String text,
        @NotBlank String source,
        @NotBlank String target
) {}
