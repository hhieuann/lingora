package com.lingora.model;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Lob;
import jakarta.persistence.Table;

import java.time.Instant;

/** Một buổi học đã lưu. segments/summary lưu dạng JSON text cho gọn. */
@Entity
@Table(name = "study_session")
public class StudySession {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String srcLang;
    private String tgtLang;
    private int durationSeconds;
    private int segmentCount;
    private int wordCount;
    private Instant createdAt;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String segmentsJson;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String summaryJson;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    public String getSrcLang() { return srcLang; }
    public void setSrcLang(String srcLang) { this.srcLang = srcLang; }
    public String getTgtLang() { return tgtLang; }
    public void setTgtLang(String tgtLang) { this.tgtLang = tgtLang; }
    public int getDurationSeconds() { return durationSeconds; }
    public void setDurationSeconds(int durationSeconds) { this.durationSeconds = durationSeconds; }
    public int getSegmentCount() { return segmentCount; }
    public void setSegmentCount(int segmentCount) { this.segmentCount = segmentCount; }
    public int getWordCount() { return wordCount; }
    public void setWordCount(int wordCount) { this.wordCount = wordCount; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public String getSegmentsJson() { return segmentsJson; }
    public void setSegmentsJson(String segmentsJson) { this.segmentsJson = segmentsJson; }
    public String getSummaryJson() { return summaryJson; }
    public void setSummaryJson(String summaryJson) { this.summaryJson = summaryJson; }
}
