package com.lingora.controller;

import com.lingora.dto.SummarizeRequest;
import com.lingora.dto.SummarizeResponse;
import com.lingora.service.SummaryService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * POST /api/summarize — {"targetLang","segments":[...]} →
 * {"points":[...],"vocab":[...],"homework":[...]}.
 */
@RestController
@RequestMapping("/api")
public class SummarizeController {

    private final SummaryService summary;

    public SummarizeController(SummaryService summary) {
        this.summary = summary;
    }

    @PostMapping("/summarize")
    public SummarizeResponse summarize(@Valid @RequestBody SummarizeRequest req) {
        return summary.summarize(req);
    }
}
