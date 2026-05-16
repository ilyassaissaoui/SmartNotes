package com.example.smartnotes.controller.api;

import com.example.smartnotes.dto.api.AiStatusResponse;
import com.example.smartnotes.service.AiNoteAssistantService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class ApiAiController {

    private final AiNoteAssistantService aiNoteAssistantService;

    @GetMapping("/status")
    public AiStatusResponse status() {
        return new AiStatusResponse(aiNoteAssistantService.isConfigured(), aiNoteAssistantService.getConfigurationHint());
    }
}
