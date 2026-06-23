package com.lingora.controller;

import com.lingora.service.AnthropicClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

/** Endpoint phụ trợ: kiểm tra backend sống & trạng thái cấu hình. */
@RestController
@RequestMapping("/api")
public class SystemController {

    private final AnthropicClient anthropic;

    public SystemController(AnthropicClient anthropic) {
        this.anthropic = anthropic;
    }

    @GetMapping("/health")
    public Map<String, String> health() {
        return Map.of("status", "ok");
    }

    /** Cho frontend biết LLM có bật không (tóm tắt/quiz bằng AI hay fallback). */
    @GetMapping("/config")
    public Map<String, Object> config() {
        return Map.of(
                "llmEnabled", anthropic.enabled(),
                "model", anthropic.model(),
                "version", "0.1.0");
    }
}
