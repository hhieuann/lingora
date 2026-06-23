package com.lingora.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.lingora.dto.SaveSessionRequest;
import com.lingora.dto.SessionListItem;
import com.lingora.dto.StudySessionResponse;
import com.lingora.model.StudySession;
import com.lingora.repo.StudySessionRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.Instant;
import java.util.List;

/** Lưu / liệt kê / mở / xoá buổi học. segments & summary lưu dạng JSON text. */
@Service
public class SessionService {

    private final StudySessionRepository repo;
    private final ObjectMapper mapper;

    public SessionService(StudySessionRepository repo, ObjectMapper mapper) {
        this.repo = repo;
        this.mapper = mapper;
    }

    public SessionListItem save(SaveSessionRequest req) {
        StudySession e = new StudySession();
        e.setTitle(req.title());
        e.setSrcLang(req.srcLang());
        e.setTgtLang(req.tgtLang());
        e.setDurationSeconds(req.durationSeconds());
        e.setCreatedAt(Instant.now());

        JsonNode segs = req.segments() == null ? mapper.createArrayNode() : req.segments();
        e.setSegmentCount(segs.isArray() ? segs.size() : 0);
        e.setWordCount(countWords(segs));
        e.setSegmentsJson(write(segs));
        e.setSummaryJson(req.summary() == null ? null : write(req.summary()));

        return toListItem(repo.save(e));
    }

    public List<SessionListItem> list() {
        return repo.findAllByOrderByCreatedAtDesc().stream().map(this::toListItem).toList();
    }

    public StudySessionResponse get(Long id) {
        StudySession e = repo.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi học " + id));
        return new StudySessionResponse(
                e.getId(), e.getTitle(), e.getSrcLang(), e.getTgtLang(),
                e.getDurationSeconds(), e.getSegmentCount(), e.getWordCount(), e.getCreatedAt(),
                read(e.getSegmentsJson()), read(e.getSummaryJson()));
    }

    public void delete(Long id) {
        if (!repo.existsById(id)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Không tìm thấy buổi học " + id);
        }
        repo.deleteById(id);
    }

    // ----- helpers -----
    private int countWords(JsonNode segs) {
        int wc = 0;
        if (segs.isArray()) {
            for (JsonNode s : segs) {
                String src = s.path("src").asText("");
                if (!src.isBlank()) wc += src.trim().split("\\s+").length;
            }
        }
        return wc;
    }

    private SessionListItem toListItem(StudySession e) {
        return new SessionListItem(e.getId(), e.getTitle(), e.getSrcLang(), e.getTgtLang(),
                e.getDurationSeconds(), e.getSegmentCount(), e.getWordCount(), e.getCreatedAt());
    }

    private String write(JsonNode node) {
        try {
            return mapper.writeValueAsString(node);
        } catch (Exception ex) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "JSON không hợp lệ: " + ex.getMessage());
        }
    }

    private JsonNode read(String json) {
        if (json == null || json.isBlank()) return null;
        try {
            return mapper.readTree(json);
        } catch (Exception ex) {
            return null;
        }
    }
}
