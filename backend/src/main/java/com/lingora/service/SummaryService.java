package com.lingora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingora.dto.SummarizeRequest;
import com.lingora.dto.SummarizeResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

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
 * - Có ANTHROPIC_API_KEY: gọi Claude (LLM thật), yêu cầu trả JSON đúng shape.
 * - Không có: extractive cơ bản (tần suất từ + cue) như frontend, để vẫn chạy được.
 */
@Service
public class SummaryService {

    private static final Logger log = LoggerFactory.getLogger(SummaryService.class);

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tóm tắt bài giảng. Đầu vào là transcript một buổi học.
            Trả về DUY NHẤT một JSON hợp lệ, không kèm giải thích, dạng:
            {"points":[...], "vocab":[{"term","lang","mean"}], "homework":[{"who","text"}]}
            - points: 3-5 ý chính, ngôn ngữ = %s.
            - vocab: 5-8 thuật ngữ quan trọng, "mean" dịch sang %s, "lang" là BCP-47 của term.
            - homework: các đầu việc/bài tập được giao; nếu không có, trả mảng rỗng.
            """;

    private final TranslationService translation;
    private final AnthropicClient anthropic;
    private final ObjectMapper mapper;

    public SummaryService(TranslationService translation, AnthropicClient anthropic, ObjectMapper mapper) {
        this.translation = translation;
        this.anthropic = anthropic;
        this.mapper = mapper;
    }

    public SummarizeResponse summarize(SummarizeRequest req) {
        List<SummarizeRequest.Segment> segments = req.segments() == null ? List.of() : req.segments();
        if (anthropic.enabled()) {
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
        String text = anthropic.complete(String.format(SYSTEM_PROMPT, targetLang, targetLang), transcript);
        return mapper.readValue(AnthropicClient.stripToJson(text), SummarizeResponse.class);
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

        List<SummarizeResponse.Vocab> vocab = wordFreq(srcText, 3).entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(6)
                .map(e -> new SummarizeResponse.Vocab(
                        e.getKey(), "en-US", translation.translate(e.getKey(), "en", targetLang)))
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
