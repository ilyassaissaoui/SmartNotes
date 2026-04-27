package com.example.smartnotes.service;

import com.example.smartnotes.dto.NoteForm;
import com.example.smartnotes.exception.NoteNotFoundException;
import com.example.smartnotes.model.Note;
import com.example.smartnotes.repository.NoteRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NoteService {

    private final NoteRepository noteRepository;
    private final CategoryService categoryService;
    private final TagService tagService;

    public List<Note> getAllNotes() {
        return noteRepository.findAll()
                .stream()
                .sorted((a, b) -> b.getUpdatedAt().compareTo(a.getUpdatedAt()))
                .toList();
    }

    public List<Note> searchNotes(String query, Long categoryId, Long tagId) {
        String cleanedQuery = StringUtils.hasText(query) ? query.trim() : null;

        if (cleanedQuery == null && categoryId == null && tagId == null) {
            return getAllNotes();
        }

        return noteRepository.search(cleanedQuery, categoryId, tagId);
    }

    public Note getNoteById(Long id) {
        return noteRepository.findById(id)
                .orElseThrow(() -> new NoteNotFoundException(id));
    }

    public long countNotes() {
        return noteRepository.count();
    }

    public List<Note> findLatestNotes() {
        return noteRepository.findTop5ByOrderByUpdatedAtDesc();
    }

    @Transactional
    public Note createNote(NoteForm form) {
        Note note = new Note();
        applyFormToNote(note, form);
        return noteRepository.save(note);
    }

    @Transactional
    public Note updateNote(Long id, NoteForm form) {
        Note note = getNoteById(id);
        applyFormToNote(note, form);
        return noteRepository.save(note);
    }

    @Transactional
    public void deleteNote(Long id) {
        if (!noteRepository.existsById(id)) {
            throw new NoteNotFoundException(id);
        }

        noteRepository.deleteTagLinksByNoteId(id);
        noteRepository.deleteById(id);
        noteRepository.flush();
    }

    private void applyFormToNote(Note note, NoteForm form) {
        note.setTitle(form.getTitle().trim());
        note.setContent(form.getContent().trim());
        note.setCategory(categoryService.getById(form.getCategoryId()));
        note.setTags(tagService.resolveTags(form.getTags()));
    }
}
