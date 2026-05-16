package com.example.smartnotes.controller.api;

import com.example.smartnotes.dto.NoteForm;
import com.example.smartnotes.dto.api.AiListResponse;
import com.example.smartnotes.dto.api.AiTextResponse;
import com.example.smartnotes.dto.api.NoteRequest;
import com.example.smartnotes.dto.api.NoteResponse;
import com.example.smartnotes.model.Note;
import com.example.smartnotes.service.NoteService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/notes")
@RequiredArgsConstructor
public class ApiNoteController {

    private final NoteService noteService;

    @GetMapping
    public List<NoteResponse> listNotes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId
    ) {
        return noteService.searchNotes(query, categoryId, tagId)
                .stream()
                .map(NoteResponse::fromEntity)
                .toList();
    }

    @GetMapping("/{id}")
    public NoteResponse getNote(@PathVariable Long id) {
        return NoteResponse.fromEntity(noteService.getNoteById(id));
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public NoteResponse createNote(@Valid @RequestBody NoteRequest request) {
        Note note = noteService.createNote(toForm(request));
        return NoteResponse.fromEntity(note);
    }

    @PutMapping("/{id}")
    public NoteResponse updateNote(@PathVariable Long id, @Valid @RequestBody NoteRequest request) {
        Note note = noteService.updateNote(id, toForm(request));
        return NoteResponse.fromEntity(note);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteNote(@PathVariable Long id) {
        noteService.deleteNote(id);
    }

    @PostMapping("/{id}/ai/summary")
    public AiTextResponse summarizeNote(@PathVariable Long id) {
        NoteService.AiTextResult result = noteService.summarizeNote(id);
        return new AiTextResponse("Summary", result.getText(), result.isFromCache());
    }

    @PostMapping("/{id}/ai/key-points")
    public AiListResponse extractKeyPoints(@PathVariable Long id) {
        NoteService.AiListResult result = noteService.extractKeyPoints(id);
        return new AiListResponse("Key points", result.getItems(), result.isFromCache());
    }

    @PostMapping("/{id}/ai/quiz")
    public AiListResponse generateQuiz(@PathVariable Long id) {
        NoteService.AiListResult result = noteService.generateQuizQuestions(id);
        return new AiListResponse("Quiz questions", result.getItems(), result.isFromCache());
    }


    @PostMapping("/{id}/ai/quiz-answers")
    public AiListResponse generateQuizAnswers(@PathVariable Long id) {
        NoteService.AiListResult result = noteService.generateQuizAnswers(id);
        return new AiListResponse("Quiz answers", result.getItems(), result.isFromCache());
    }

    private NoteForm toForm(NoteRequest request) {
        NoteForm form = new NoteForm();
        form.setTitle(request.getTitle());
        form.setContent(request.getContent());
        form.setCategoryId(request.getCategoryId());
        form.setTags(request.getTags());
        return form;
    }
}
