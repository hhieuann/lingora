package com.lingora.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Client gọi Anthropic Claude API (dùng chung cho tóm tắt + quiz).
 * Key đặt qua biến môi trường ANTHROPIC_API_KEY — KHÔNG hardcode.
 */
@Component
public class AnthropicClient {

    private static final String URL = "https://api.anthropic.com/v1/messages";
    private final RestClient http = RestClient.create();

    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public AnthropicClient(
            @Value("${lingora.anthropic.api-key:}") String apiKey,
            @Value("${lingora.anthropic.model:claude-sonnet-4-6}") String model,
            @Value("${lingora.anthropic.max-tokens:1500}") int maxTokens) {
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    /** Có cấu hình key để gọi LLM không. */
    public boolean enabled() {
        return apiKey != null && !apiKey.isBlank();
    }

    public String model() {
        return model;
    }

    /** Gọi Claude với system + user, trả về text của block đầu tiên. */
    public String complete(String system, String user) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", system);
        body.put("messages", List.of(Map.of("role", "user", "content", user)));

        JsonNode resp = http.post().uri(URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        if (resp != null) {
            for (JsonNode block : resp.path("content")) {
                if ("text".equals(block.path("type").asText())) {
                    return block.path("text").asText("");
                }
            }
        }
        return "";
    }

    /** Bỏ ```json ... ``` và lấy phần JSON ({...}) nếu LLM trả kèm giải thích. */
    public static String stripToJson(String s) {
        if (s == null) return "{}";
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        return (start >= 0 && end >= start) ? t.substring(start, end + 1) : t;
    }
}
