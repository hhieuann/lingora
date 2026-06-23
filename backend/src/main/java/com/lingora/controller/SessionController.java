package com.lingora.controller;

import com.lingora.dto.SaveSessionRequest;
import com.lingora.dto.SessionListItem;
import com.lingora.dto.StudySessionResponse;
import com.lingora.service.SessionService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** CRUD buổi học đã lưu. */
@RestController
@RequestMapping("/api/sessions")
public class SessionController {

    private final SessionService sessions;

    public SessionController(SessionService sessions) {
        this.sessions = sessions;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public SessionListItem save(@Valid @RequestBody SaveSessionRequest req) {
        return sessions.save(req);
    }

    @GetMapping
    public List<SessionListItem> list() {
        return sessions.list();
    }

    @GetMapping("/{id}")
    public StudySessionResponse get(@PathVariable Long id) {
        return sessions.get(id);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void delete(@PathVariable Long id) {
        sessions.delete(id);
    }
}
