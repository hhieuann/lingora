package com.lingora.dto;

import java.util.List;

/** Quiz: danh sách câu hỏi trắc nghiệm. answerIndex = vị trí đáp án đúng trong options. */
public record QuizResponse(List<Question> questions) {
    public record Question(String question, List<String> options, int answerIndex, String explanation) {}
}
