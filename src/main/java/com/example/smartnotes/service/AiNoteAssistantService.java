package com.example.smartnotes.service;

import java.util.List;

public interface AiNoteAssistantService {

    boolean isConfigured();

    String getConfigurationHint();

    String summarizeNote(String content);

    List<String> extractKeyPoints(String content);

    List<String> generateQuizQuestions(String content);
}
