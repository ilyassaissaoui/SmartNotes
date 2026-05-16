package com.example.smartnotes.service;

import com.example.smartnotes.dto.NoteForm;
import com.example.smartnotes.exception.NoteNotFoundException;
import com.example.smartnotes.model.AppUser;
import com.example.smartnotes.model.Note;
import com.example.smartnotes.repository.NoteRepository;
import com.example.smartnotes.security.CurrentUserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.DigestUtils;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final AiNoteAssistantService aiNoteAssistantService;
    private final CurrentUserService currentUserService;

    public List<Note> getAllNotes() {
        return noteRepository.findByOwnerOrderByUpdatedAtDesc(currentUserService.getCurrentUser());
    }

    public List<Note> searchNotes(String query, Long categoryId, Long tagId) {
        String cleanedQuery = StringUtils.hasText(query) ? query.trim() : null;
        AppUser currentUser = currentUserService.getCurrentUser();

        if (cleanedQuery == null && categoryId == null && tagId == null) {
            return noteRepository.findByOwnerOrderByUpdatedAtDesc(currentUser);
        }

        return noteRepository.searchByOwner(currentUser, cleanedQuery, categoryId, tagId);
    }

    public Note getNoteById(Long id) {
        return noteRepository.findByIdAndOwner(id, currentUserService.getCurrentUser())
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public long countNotes() {
        return noteRepository.countByOwner(currentUserService.getCurrentUser());
    }

    public List<Note> findLatestNotes() {
        return noteRepository.findTop5ByOwnerOrderByUpdatedAtDesc(currentUserService.getCurrentUser());
    }

    @Transactional
    public Note createNote(NoteForm form) {
        Note note = new Note();
        note.setOwner(currentUserService.getCurrentUser());
        applyFormToNote(note, form);
        return noteRepository.save(note);
    }

    @Transactional
    public Note updateNote(Long id, NoteForm form) {
        Note note = getNoteById(id);
        String previousContent = note.getContent();
        applyFormToNote(note, form);

        if (!note.getContent().equals(previousContent)) {
            note.clearAiCache();
        }

        return noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long id) {
        Note note = getNoteById(id);
        noteRepository.deleteTagLinksByNoteId(note.getId());
        noteRepository.delete(note);
        noteRepository.flush();
    }

    @Transactional
    public AiTextResult summarizeNote(Long id) {
        Note note = getNoteById(id);
        String hash = contentHash(note.getContent());

        if (hash.equals(note.getAiContentHash()) && StringUtils.hasText(note.getAiSummary())) {
            return new AiTextResult(note.getAiSummary(), true);
        }

        String summary = aiNoteAssistantService.summarizeNote(note.getContent());
        note.setAiContentHash(hash);
        note.setAiSummary(summary);
        noteRepository.save(note);
        return new AiTextResult(summary, false);
    }

    @Transactional
    public AiListResult extractKeyPoints(Long id) {
        Note note = getNoteById(id);
        String hash = contentHash(note.getContent());

        if (hash.equals(note.getAiContentHash()) && StringUtils.hasText(note.getAiKeyPoints())) {
            return new AiListResult(splitLines(note.getAiKeyPoints()), true);
        }

        List<String> keyPoints = aiNoteAssistantService.extractKeyPoints(note.getContent());
        note.setAiContentHash(hash);
        note.setAiKeyPoints(joinLines(keyPoints));
        noteRepository.save(note);
        return new AiListResult(keyPoints, false);
    }

    @Transactional
    public AiListResult generateQuizQuestions(Long id) {
        Note note = getNoteById(id);
        String hash = contentHash(note.getContent());

        if (hash.equals(note.getAiContentHash()) && StringUtils.hasText(note.getAiQuizQuestions())) {
            return new AiListResult(splitLines(note.getAiQuizQuestions()), true);
        }

        List<String> quizQuestions = aiNoteAssistantService.generateQuizQuestions(note.getContent());
        note.setAiContentHash(hash);
        note.setAiQuizQuestions(joinLines(quizQuestions));
        noteRepository.save(note);
        return new AiListResult(quizQuestions, false);
    }


    @Transactional
    public AiListResult generateQuizAnswers(Long id) {
        Note note = getNoteById(id);
        String hash = contentHash(note.getContent());

        if (hash.equals(note.getAiContentHash()) && StringUtils.hasText(note.getAiQuizAnswers())) {
            return new AiListResult(splitLines(note.getAiQuizAnswers()), true);
        }

        List<String> questions;
        if (hash.equals(note.getAiContentHash()) && StringUtils.hasText(note.getAiQuizQuestions())) {
            questions = splitLines(note.getAiQuizQuestions());
        } else {
            questions = aiNoteAssistantService.generateQuizQuestions(note.getContent());
            note.setAiContentHash(hash);
            note.setAiQuizQuestions(joinLines(questions));
        }

        List<String> answers = aiNoteAssistantService.generateQuizAnswers(note.getContent(), questions);
        note.setAiContentHash(hash);
        note.setAiQuizAnswers(joinLines(answers));
        noteRepository.save(note);
        return new AiListResult(answers, false);
    }

    private void applyFormToNote(Note note, NoteForm form) {
        note.setTitle(form.getTitle().trim());
        note.setContent(form.getContent().trim());
        note.setCategory(categoryService.getById(form.getCategoryId()));
        note.setTags(tagService.resolveTags(form.getTags()));
    }

    private String contentHash(String content) {
        return DigestUtils.md5DigestAsHex(content.getBytes(StandardCharsets.UTF_8));
    }

    private String joinLines(List<String> values) {
        return String.join("\n", values);
    }

    private List<String> splitLines(String text) {
        return Arrays.stream(text.split("\r?\n"))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .toList();
    }

    public static class AiTextResult {
        private final String text;
        private final boolean fromCache;

        public AiTextResult(String text, boolean fromCache) {
            this.text = text;
            this.fromCache = fromCache;
        }

        public String getText() {
            return text;
        }

        public boolean isFromCache() {
            return fromCache;
        }
    }

    public static class AiListResult {
        private final List<String> items;
        private final boolean fromCache;

        public AiListResult(List<String> items, boolean fromCache) {
            this.items = items;
            this.fromCache = fromCache;
        }

        public List<String> getItems() {
            return items;
        }

        public boolean isFromCache() {
            return fromCache;
        }
    }
}
