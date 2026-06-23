package com.lingora.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingora.dto.QuizRequest;
import com.lingora.dto.QuizResponse;
import com.lingora.dto.SummarizeRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * Tạo câu hỏi trắc nghiệm ôn tập từ transcript.
 * - Có key: Claude sinh câu hỏi đúng nội dung (LLM thật).
 * - Không key: fallback tạo câu hỏi "thuật ngữ X nghĩa là gì?" từ từ vựng.
 */
@Service
public class QuizService {

    private static final Logger log = LoggerFactory.getLogger(QuizService.class);

    private static final String SYSTEM_PROMPT = """
            Bạn là trợ lý tạo câu hỏi ôn tập. Đầu vào là transcript một buổi học.
            Trả về DUY NHẤT một JSON hợp lệ, không kèm giải thích, dạng:
            {"questions":[{"question","options":["A","B","C","D"],"answerIndex":0,"explanation"}]}
            - Tạo đúng %d câu hỏi trắc nghiệm, mỗi câu 4 phương án, chỉ 1 đúng.
            - answerIndex là chỉ số (0-3) của phương án đúng trong "options".
            - Ngôn ngữ của câu hỏi và phương án = %s. Bám sát nội dung bài học.
            """;

    private final AnthropicClient anthropic;
    private final TranslationService translation;
    private final ObjectMapper mapper;
    private final Random rng = new Random();

    public QuizService(AnthropicClient anthropic, TranslationService translation, ObjectMapper mapper) {
        this.anthropic = anthropic;
        this.translation = translation;
        this.mapper = mapper;
    }

    public QuizResponse generate(QuizRequest req) {
        int count = (req.count() == null) ? 5 : Math.max(1, Math.min(10, req.count()));
        List<SummarizeRequest.Segment> segments = req.segments() == null ? List.of() : req.segments();

        if (anthropic.enabled()) {
            try {
                return generateWithClaude(req.targetLang(), count, segments);
            } catch (Exception e) {
                log.warn("Claude quiz lỗi, fallback: {}", e.getMessage());
            }
        }
        return generateFallback(req.targetLang(), count, segments);
    }

    private QuizResponse generateWithClaude(String targetLang, int count, List<SummarizeRequest.Segment> segments) throws Exception {
        String transcript = segments.stream()
                .map(s -> (s.speaker() == null ? "" : s.speaker() + ": ") + (s.src() == null ? "" : s.src()))
                .collect(Collectors.joining("\n"));
        String text = anthropic.complete(String.format(SYSTEM_PROMPT, count, targetLang), transcript);
        return mapper.readValue(AnthropicClient.stripToJson(text), QuizResponse.class);
    }

    // ----- Fallback: MCQ từ từ vựng -----
    private static final Set<String> STOP = Set.of(
            "the", "a", "an", "and", "or", "but", "to", "of", "in", "on", "for", "is", "are", "was", "were",
            "be", "will", "would", "can", "could", "should", "that", "this", "with", "from", "they", "you", "we");
    private static final Pattern WORDS = Pattern.compile("[^\\p{L}\\p{N}]+");

    private QuizResponse generateFallback(String targetLang, int count, List<SummarizeRequest.Segment> segments) {
        String srcText = segments.stream().map(s -> s.src() == null ? "" : s.src()).collect(Collectors.joining(" "));
        List<String> terms = topTerms(srcText, count + 3);

        if (terms.size() < 4) {
            return new QuizResponse(List.of(new QuizResponse.Question(
                    "Chưa đủ nội dung để tạo câu hỏi. Hãy ghi/nói thêm, hoặc đặt ANTHROPIC_API_KEY để tạo quiz bằng AI.",
                    List.of("Đã hiểu"), 0, "")));
        }

        Map<String, String> mean = new LinkedHashMap<>();
        for (String t : terms) mean.put(t, translation.translate(t, "en", targetLang));

        List<QuizResponse.Question> questions = new ArrayList<>();
        int n = Math.min(count, terms.size());
        for (int i = 0; i < n; i++) {
            String term = terms.get(i);
            String correct = mean.get(term);
            List<String> options = new ArrayList<>();
            options.add(correct);
            for (String t : terms) {
                if (options.size() >= 4) break;
                String m = mean.get(t);
                if (!t.equals(term) && !options.contains(m)) options.add(m);
            }
            Collections.shuffle(options, rng);
            questions.add(new QuizResponse.Question(
                    "Trong bài học, thuật ngữ \"" + term + "\" có nghĩa là gì?",
                    options, options.indexOf(correct), "“" + term + "” = " + correct));
        }
        return new QuizResponse(questions);
    }

    private List<String> topTerms(String text, int n) {
        Map<String, Integer> freq = new LinkedHashMap<>();
        for (String w : WORDS.split(text.toLowerCase())) {
            if (w.length() > 3 && !STOP.contains(w)) freq.merge(w, 1, Integer::sum);
        }
        return freq.entrySet().stream()
                .sorted((a, b) -> b.getValue() - a.getValue())
                .limit(n)
                .map(Map.Entry::getKey)
                .toList();
    }
}
