package com.lingora.dto;

/** Phản hồi dịch: {"translatedText"} — khớp với hàm translate() ở frontend. */
public record TranslateResponse(String translatedText) {}
