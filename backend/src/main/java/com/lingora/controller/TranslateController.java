package com.lingora.controller;

import com.lingora.dto.TranslateRequest;
import com.lingora.dto.TranslateResponse;
import com.lingora.service.TranslationService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** POST /api/translate — {"text","source","target"} → {"translatedText"}. */
@RestController
@RequestMapping("/api")
public class TranslateController {

    private final TranslationService translation;

    public TranslateController(TranslationService translation) {
        this.translation = translation;
    }

    @PostMapping("/translate")
    public TranslateResponse translate(@Valid @RequestBody TranslateRequest req) {
        String out = translation.translate(req.text(), req.source(), req.target());
        return new TranslateResponse(out);
    }
}
