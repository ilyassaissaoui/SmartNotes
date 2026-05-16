package com.example.smartnotes.dto.api;

import com.example.smartnotes.model.Note;
import com.example.smartnotes.model.Tag;

import java.time.LocalDateTime;
import java.util.List;

public class NoteResponse {

    private Long id;
    private String title;
    private String content;
    private CategoryResponse category;
    private List<String> tags;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private boolean hasCachedSummary;
    private boolean hasCachedKeyPoints;
    private boolean hasCachedQuizQuestions;

    public NoteResponse() {
    }

    public static NoteResponse fromEntity(Note note) {
        NoteResponse response = new NoteResponse();
        response.setId(note.getId());
        response.setTitle(note.getTitle());
        response.setContent(note.getContent());
        response.setCategory(CategoryResponse.fromEntity(note.getCategory()));
        response.setTags(note.getTags().stream().map(Tag::getName).sorted(String::compareToIgnoreCase).toList());
        response.setCreatedAt(note.getCreatedAt());
        response.setUpdatedAt(note.getUpdatedAt());
        response.setHasCachedSummary(note.getAiSummary() != null && !note.getAiSummary().isBlank());
        response.setHasCachedKeyPoints(note.getAiKeyPoints() != null && !note.getAiKeyPoints().isBlank());
        response.setHasCachedQuizQuestions(note.getAiQuizQuestions() != null && !note.getAiQuizQuestions().isBlank());
        return response;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }

    public CategoryResponse getCategory() {
        return category;
    }

    public void setCategory(CategoryResponse category) {
        this.category = category;
    }

    public List<String> getTags() {
        return tags;
    }

    public void setTags(List<String> tags) {
        this.tags = tags;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public boolean isHasCachedSummary() {
        return hasCachedSummary;
    }

    public void setHasCachedSummary(boolean hasCachedSummary) {
        this.hasCachedSummary = hasCachedSummary;
    }

    public boolean isHasCachedKeyPoints() {
        return hasCachedKeyPoints;
    }

    public void setHasCachedKeyPoints(boolean hasCachedKeyPoints) {
        this.hasCachedKeyPoints = hasCachedKeyPoints;
    }

    public boolean isHasCachedQuizQuestions() {
        return hasCachedQuizQuestions;
    }

    public void setHasCachedQuizQuestions(boolean hasCachedQuizQuestions) {
        this.hasCachedQuizQuestions = hasCachedQuizQuestions;
    }
}
