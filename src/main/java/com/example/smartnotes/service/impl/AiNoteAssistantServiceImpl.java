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

    private final ChatClient chatClient;
    private final String aiProvider;
    private final String openAiApiKey;
    private final String openAiBaseUrl;
    private final String openAiModel;
    private final String ollamaBaseUrl;
    private final String ollamaModel;
    private final int maxInputCharacters;

    public AiNoteAssistantServiceImpl(
            ObjectProvider<ChatClient.Builder> chatClientBuilderProvider,
            @Value("${AI_PROVIDER:${spring.ai.model.chat:none}}") String aiProvider,
            @Value("${OPENAI_API_KEY:${spring.ai.openai.api-key:}}") String openAiApiKey,
            @Value("${OPENAI_BASE_URL:${spring.ai.openai.base-url:}}") String openAiBaseUrl,
            @Value("${OPENAI_MODEL:${spring.ai.openai.chat.options.model:}}") String openAiModel,
            @Value("${OLLAMA_BASE_URL:${spring.ai.ollama.base-url:}}") String ollamaBaseUrl,
            @Value("${OLLAMA_MODEL:${spring.ai.ollama.chat.options.model:}}") String ollamaModel,
            @Value("${AI_MAX_INPUT_CHARACTERS:4500}") int maxInputCharacters
    ) {
        ChatClient.Builder builder = chatClientBuilderProvider.getIfAvailable();
        this.chatClient = builder == null ? null : builder.build();
        this.aiProvider = normalizeProvider(aiProvider, "none");
        this.openAiApiKey = clean(openAiApiKey, "");
        this.openAiBaseUrl = clean(openAiBaseUrl, "");
        this.openAiModel = clean(openAiModel, "");
        this.ollamaBaseUrl = clean(ollamaBaseUrl, "");
        this.ollamaModel = clean(ollamaModel, "");
        this.maxInputCharacters = Math.max(1000, maxInputCharacters);
    }

    @Override
    public boolean isConfigured() {
        if (chatClient == null) {
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
            case "openai" -> {
                if (!StringUtils.hasText(openAiApiKey) || isPlaceholder(openAiApiKey)) {
                    yield "AI API mode is selected. Set OPENAI_API_KEY. For Gemini, use a Gemini API key with OPENAI_BASE_URL=https://generativelanguage.googleapis.com/v1beta/openai.";
                }
                yield "AI API is configured using model " + fallback(openAiModel, "unknown") + " at " + fallback(openAiBaseUrl, "default OpenAI base URL") + ".";
            }
            case "ollama" -> "Ollama is selected. Make sure OLLAMA_BASE_URL and OLLAMA_MODEL are set and that Ollama is running.";
            default -> "AI is disabled. Set AI_PROVIDER=openai for Gemini/OpenAI-compatible APIs, or AI_PROVIDER=ollama for local Ollama.";
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
                Return a concise summary in 3 to 5 complete sentences.
                Keep the answer under 140 words.
                Do not add headings, markdown, or bullet points.
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


    @Override
    public List<String> generateQuizAnswers(String content, List<String> questions) {
        validateContent(content);

        if (questions == null || questions.isEmpty()) {
            throw new AiFeatureException("Quiz questions are required to generate answers.");
        }

        StringBuilder numberedQuestions = new StringBuilder();
        for (int i = 0; i < questions.size(); i++) {
            numberedQuestions.append(i + 1).append(". ").append(questions.get(i).trim()).append("\n");
        }

        String userPrompt = buildNotePrompt(content)
                + "\n\nHere are the quiz questions:\n"
                + numberedQuestions
                + "\nAnswer these exact questions in the same order.";

        String response = askModel(
                """
                You are a helpful study assistant.
                Use only the note content provided by the user.
                Do not invent facts.
                Always respond in the same language as the note content and the quiz questions.
                Answer each numbered question with a complete answer, not only keywords.
                Each answer must be 1 to 3 clear sentences, enough for a student to understand the correction.
                If the correct answer is a name, date, or term, include a short explanation when useful.
                Output only the answers, one answer per line, in the same order as the questions.
                Do not repeat the questions. Do not add numbering or extra formatting.
                """,
                userPrompt
        );

        List<String> answers = parseLineList(response, "No quiz answers were generated.");

        if (answers.size() > questions.size()) {
            return answers.subList(0, questions.size());
        }

        if (answers.size() < questions.size()) {
            throw new AiFeatureException("The AI did not return enough quiz answers. Please try again.");
        }

        return answers;
    }

    private String askModel(String systemPrompt, String userPrompt) {
        if (!isConfigured()) {
            throw new AiFeatureException(getConfigurationHint());
        }

        try {
            String response = chatClient
                    .prompt()
                    .system(systemPrompt)
                    .user(userPrompt)
                    .call()
                    .content();

            if (!StringUtils.hasText(response)) {
                throw new AiFeatureException("The AI returned an empty response. Please try again.");
            }

            return response.trim();
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
        String cleaned = content.trim();
        if (cleaned.length() > maxInputCharacters) {
            cleaned = cleaned.substring(0, maxInputCharacters)
                    + "\n\n[The note was longer than the configured AI input limit, so only the first "
                    + maxInputCharacters
                    + " characters were sent.]";
        }

        return "Here is the note content:\n\n" + cleaned;
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

    private String clean(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.trim();
    }

    private String normalizeProvider(String value, String fallback) {
        if (value == null) {
            return fallback;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }

    private String fallback(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private boolean isPlaceholder(String value) {
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return normalized.isBlank()
                || normalized.equals("your-key")
                || normalized.equals("changeme")
                || normalized.equals("change-me")
                || normalized.equals("replace-me")
                || normalized.equals("replace_with_key")
                || normalized.equals("your_gemini_api_key")
                || normalized.equals("not-configured")
                || normalized.equals("dummy")
                || normalized.equals("missing");
    }
}
