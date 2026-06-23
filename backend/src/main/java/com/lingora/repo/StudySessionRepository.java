package com.lingora.repo;

import com.lingora.model.StudySession;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface StudySessionRepository extends JpaRepository<StudySession, Long> {
    /** Danh sách buổi học mới nhất trước. */
    List<StudySession> findAllByOrderByCreatedAtDesc();
}
