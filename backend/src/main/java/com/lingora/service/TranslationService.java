package com.lingora.service;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

/**
 * Dịch qua MyMemory (free, không cần key) — gọi từ server để né CORS/giới hạn của client.
 * Đổi provider khác (DeepL/Google/LibreTranslate) chỉ cần sửa hàm {@link #translate}.
 */
@Service
public class TranslationService {

    private static final String MYMEMORY = "https://api.mymemory.translated.net/get";
    private final RestClient http = RestClient.create();

    /** Trả về chuỗi đã dịch; nếu lỗi/không dịch được thì trả lại text gốc. */
    public String translate(String text, String source, String target) {
        if (text == null || text.isBlank() || source.equals(target)) {
            return text;
        }
        try {
            URI uri = UriComponentsBuilder.fromUriString(MYMEMORY)
                    .queryParam("q", text)
                    .queryParam("langpair", source + "|" + target)
                    .build()
                    .encode()
                    .toUri();
            JsonNode body = http.get().uri(uri).retrieve().body(JsonNode.class);
            if (body != null) {
                String out = body.path("responseData").path("translatedText").asText("");
                if (!out.isBlank() && !out.matches("(?i).*(MYMEMORY WARNING|QUOTA).*")) {
                    return out;
                }
            }
        } catch (Exception e) {
            // Im lặng nuốt lỗi mạng — trả text gốc để app không vỡ.
        }
        return text;
    }
}
