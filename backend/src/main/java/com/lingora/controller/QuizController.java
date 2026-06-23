package com.lingora.controller;

import com.lingora.dto.QuizRequest;
import com.lingora.dto.QuizResponse;
import com.lingora.service.QuizService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/quiz — tạo câu hỏi ôn tập từ transcript. */
@RestController
@RequestMapping("/api")
public class QuizController {

    private final QuizService quiz;

    public QuizController(QuizService quiz) {
        this.quiz = quiz;
    }

    @PostMapping("/quiz")
    public QuizResponse quiz(@Valid @RequestBody QuizRequest req) {
        return quiz.generate(req);
    }
}
