package com.example.smartnotes.service.impl;

import com.example.smartnotes.exception.AiFeatureException;
import com.example.smartnotes.service.AiNoteAssistantService;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.List;
import java.util.Locale;

@Service
public class AiNoteAssistantServiceImpl implements AiNoteAssistantService {

    private final ChatClient.Builder chatClientBuilder;
    private final String aiProvider;
    private final String openAiApiKey;
    private final String ollamaBaseUrl;
    private final String ollamaModel;

    public AiNoteAssistantServiceImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${AI_PROVIDER:none}") String aiProvider,
            @Value("${OPENAI_API_KEY:}") String openAiApiKey,
            @Value("${OLLAMA_BASE_URL:}") String ollamaBaseUrl,
            @Value("${OLLAMA_MODEL:}") String ollamaModel
    ) {
        this.chatClientBuilder = chatClientBuilderProvider.getIfAvailable();
        this.aiProvider = normalize(aiProvider, "none");
        this.openAiApiKey = normalize(openAiApiKey, "");
        this.ollamaBaseUrl = normalize(ollamaBaseUrl, "");
        this.ollamaModel = normalize(ollamaModel, "");
    }

    @Override
    public boolean isConfigured() {
        if (chatClientBuilder == null) {
            return false;
        }

        return switch (aiProvider) {
            case "openai" -> StringUtils.hasText(openAiApiKey) && !isPlaceholder(openAiApiKey);
            case "ollama" -> StringUtils.hasText(ollamaBaseUrl) && StringUtils.hasText(ollamaModel);
            default -> false;
        };
    }

    @Override
    public String getConfigurationHint() {
        return switch (aiProvider) {
            case "openai" -> "OpenAI is selected, but OPENAI_API_KEY is missing or still set to a placeholder value.";
            case "ollama" -> "Ollama is selected. Make sure OLLAMA_BASE_URL and OLLAMA_MODEL are set and that Ollama is running.";
            default -> "AI is disabled. Set AI_PROVIDER=openai or AI_PROVIDER=ollama and configure the matching settings.";
        };
    }

    @Override
    public String summarizeNote(String content) {
        validateContent(content);

        return askModel(
                """
                You are a helpful study assistant.
                Use only the note content provided by the user.
                Do not invent facts.
                Return a concise summary in 3 to 5 sentences.
                Do not add headings.
                """,
                buildNotePrompt(content)
        );
    }

    @Override
    public List<String> extractKeyPoints(String content) {
        validateContent(content);

        String response = askModel(
                """
                You are a helpful study assistant.
                Use only the note content provided by the user.
                Do not invent facts.
                Return 4 to 6 short bullet points.
                Output only the bullet points, one per line.
                """,
                buildNotePrompt(content)
        );

        return parseLineList(response, "No key points were generated.");
    }

    @Override
    public List<String> generateQuizQuestions(String content) {
        validateContent(content);

        String response = askModel(
                """
                You are a helpful study assistant.
                Use only the note content provided by the user.
                Do not invent facts.
                Generate exactly 3 quiz questions.
                Output only the 3 questions, one per line.
                Do not include answers.
                """,
                buildNotePrompt(content)
        );

        List<String> questions = parseLineList(response, "No quiz questions were generated.");

        if (questions.size() > 3) {
            return questions.subList(0, 3);
        }

        if (questions.size() < 3) {
            throw new AiFeatureException("The AI did not return exactly 3 quiz questions. Please try again.");
        }

        return questions;
    }

    private String askModel(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new AiFeatureException(getConfigurationHint());
        }

        try {
            return chatClientBuilder
                    .build()
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();
        } catch (Exception ex) {
            throw new AiFeatureException("The AI service is currently unavailable. Please check your AI configuration and try again.", ex);
        }
    }

    private void validateContent(String content) {
        if (!StringUtils.hasText(content) || content.trim().length() < 20) {
            throw new AiFeatureException("The note is too short for AI analysis. Add more content and try again.");
        }
    }

    private String buildNotePrompt(String content) {
        return "Here is the note content:\n\n" + content.trim();
    }

    private List<String> parseLineList(String response, String fallbackMessage) {
        if (!StringUtils.hasText(response)) {
            throw new AiFeatureException(fallbackMessage);
        }

        List<String> lines = Arrays.stream(response.split("\r?\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(this::cleanListPrefix)
                .filter(StringUtils::hasText)
                .toList();

        if (lines.isEmpty()) {
            throw new AiFeatureException(fallbackMessage);
        }

        return lines;
    }

    private String cleanListPrefix(String line) {
        return line.replaceFirst("^[-*•\\d.)\\s]+", "").trim();
    }

    private String normalize(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank()
                || normalized.equals("your-key")
                || normalized.equals("changeme")
                || normalized.equals("change-me")
                || normalized.equals("replace-me")
                || normalized.equals("replace_with_key");
    }
}
