package com.lingora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingora.dto.SummarizeRequest;
import com.lingora.dto.SummarizeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tóm tắt transcript bài giảng.
 * - Nếu có ANTHROPIC_API_KEY: gọi Claude API, yêu cầu trả JSON đúng shape (LLM thật).
 * - Nếu không: dùng extractive cơ bản (tần suất từ + cue) như frontend, để vẫn chạy được.
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);
    private static final String ANTHROPIC_URL = "https://api.anthropic.com/v1/messages";

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tóm tắt bài giảng. Đầu vào là transcript một buổi học.
            Trả về DUY NHẤT một JSON hợp lệ, không kèm giải thích, dạng:
            {"points":[...], "vocab":[{"term","lang","mean"}], "homework":[{"who","text"}]}
            - points: 3-5 ý chính, ngôn ngữ = %s.
            - vocab: 5-8 thuật ngữ quan trọng, "mean" dịch sang %s, "lang" là BCP-47 của term.
            - homework: các đầu việc/bài tập được giao; nếu không có, trả mảng rỗng.
            """;

    private final TranslationService translation;
    private final ObjectMapper mapper = new ObjectMapper();
    private final RestClient http = RestClient.create();

    private final String apiKey;
    private final String model;
    private final int maxTokens;

    public SummaryService(
            TranslationService translation,
            @Value("${lingora.anthropic.api-key:}") String apiKey,
            @Value("${lingora.anthropic.model:claude-sonnet-4-6}") String model,
            @Value("${lingora.anthropic.max-tokens:1500}") int maxTokens) {
        this.translation = translation;
        this.apiKey = apiKey;
        this.model = model;
        this.maxTokens = maxTokens;
    }

    public SummarizeResponse summarize(SummarizeRequest req) {
        List<SummarizeRequest.Segment> segments = req.segments() == null ? List.of() : req.segments();
        if (apiKey != null && !apiKey.isBlank()) {
            try {
                return summarizeWithClaude(req.targetLang(), segments);
            } catch (Exception e) {
                log.warn("Claude summarize lỗi, fallback extractive: {}", e.getMessage());
            }
        }
        return summarizeExtractive(req.targetLang(), segments);
    }

    // ---------- LLM thật (Anthropic Claude) ----------
    private SummarizeResponse summarizeWithClaude(String targetLang, List<SummarizeRequest.Segment> segments) throws Exception {
        String transcript = segments.stream()
                .map(s -> (s.speaker() == null ? "" : s.speaker() + ": ") + (s.src() == null ? "" : s.src()))
                .collect(Collectors.joining("\n"));

        Map<String, Object> body = new LinkedHashMap<>();
        body.put("model", model);
        body.put("max_tokens", maxTokens);
        body.put("system", String.format(SYSTEM_PROMPT, targetLang, targetLang));
        body.put("messages", List.of(Map.of("role", "user", "content", transcript)));

        JsonNode resp = http.post().uri(ANTHROPIC_URL)
                .header("x-api-key", apiKey)
                .header("anthropic-version", "2023-06-01")
                .contentType(MediaType.APPLICATION_JSON)
                .body(body)
                .retrieve()
                .body(JsonNode.class);

        String text = extractText(resp);
        String json = stripJsonFences(text);
        return mapper.readValue(json, SummarizeResponse.class);
    }

    /** Lấy text từ block content đầu tiên kiểu "text". */
    private String extractText(JsonNode resp) {
        if (resp == null) return "";
        for (JsonNode block : resp.path("content")) {
            if ("text".equals(block.path("type").asText())) {
                return block.path("text").asText("");
            }
        }
        return "";
    }

    /** Bỏ ```json ... ``` nếu LLM trả kèm code fence. */
    private String stripJsonFences(String s) {
        if (s == null) return "{}";
        String t = s.trim();
        if (t.startsWith("```")) {
            t = t.replaceFirst("^```[a-zA-Z]*\\s*", "").replaceFirst("```\\s*$", "").trim();
        }
        int start = t.indexOf('{');
        int end = t.lastIndexOf('}');
        return (start >= 0 && end >= start) ? t.substring(start, end + 1) : t;
    }

    // ---------- Fallback extractive (không cần key) ----------
    private static final Set<String> STOP = Set.of(
            "và", "là", "của", "có", "các", "một", "những", "được", "cho", "với", "từ", "này", "đó", "khi", "nếu", "thì", "để",
            "the", "a", "an", "and", "or", "but", "to", "of", "in", "on", "for", "is", "are", "was", "were", "be",
            "will", "would", "can", "could", "should", "i", "you", "we", "they", "it", "this", "that");
    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");
    private static final Pattern SENTENCE = Pattern.compile("(?<=[.!?。！？])\\s+");
    private static final Pattern CUES = Pattern.compile(
            "(cần|sẽ|phải|nên|hãy|deadline|bài tập|homework|làm|chuẩn bị|ôn|nộp|hoàn thành)",
            Pattern.CASE_INSENSITIVE);

    private SummarizeResponse summarizeExtractive(String targetLang, List<SummarizeRequest.Segment> segments) {
        String text = segments.stream()
                .map(s -> (s.tgt() != null && !s.tgt().isBlank()) ? s.tgt() : (s.src() == null ? "" : s.src()))
                .collect(Collectors.joining(" "));
        String srcText = segments.stream().map(s -> s.src() == null ? "" : s.src()).collect(Collectors.joining(" "));

        List<String> sentences = Arrays.stream(SENTENCE.split(text))
                .map(String::trim).filter(s -> s.length() > 4).toList();

        Map<String, Integer> freq = wordFreq(text, 2);
        List<String> points = sentences.stream()
                .sorted((a, b) -> Double.compare(score(b, freq), score(a, freq)))
                .limit(4)
                .toList();
        if (points.isEmpty()) {
            points = List.of("Chưa đủ nội dung để tóm tắt — hãy ghi/nói thêm.");
        }

        List<SummarizeResponse.Homework> homework = sentences.stream()
                .filter(s -> CUES.matcher(s).find())
                .limit(5)
                .map(s -> new SummarizeResponse.Homework("", s))
                .collect(Collectors.toCollection(ArrayList::new));
        if (homework.isEmpty()) {
            homework.add(new SummarizeResponse.Homework("", "Không phát hiện bài tập/đầu việc rõ ràng."));
        }

        String srcLang = segments.isEmpty() ? "en" : "en"; // term thường là tiếng nguồn
        List<SummarizeResponse.Vocab> vocab = wordFreq(srcText, 3).entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(6)
                .map(e -> new SummarizeResponse.Vocab(
                        e.getKey(), srcLang + "-US", translation.translate(e.getKey(), "en", targetLang)))
                .toList();

        return new SummarizeResponse(points, vocab, homework);
    }

    private Map<String, Integer> wordFreq(String text, int minLen) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String w : WORDS.split(text.toLowerCase())) {
            if (w.length() > minLen && !STOP.contains(w)) {
                freq.merge(w, 1, Integer::sum);
            }
        }
        return freq;
    }

    private double score(String sentence, Map<String, Integer> freq) {
        String[] ws = WORDS.split(sentence.toLowerCase());
        int sum = 0;
        for (String w : ws) sum += freq.getOrDefault(w, 0);
        return sum / Math.sqrt(Math.max(1, ws.length));
    }
}
