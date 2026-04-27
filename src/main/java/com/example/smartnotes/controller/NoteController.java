package com.example.smartnotes.controller;

import com.example.smartnotes.dto.NoteForm;
import com.example.smartnotes.exception.AiFeatureException;
import com.example.smartnotes.model.Note;
import com.example.smartnotes.service.AiNoteAssistantService;
import com.example.smartnotes.service.CategoryService;
import com.example.smartnotes.service.NoteService;
import com.example.smartnotes.service.TagService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.util.List;

@Controller
@RequestMapping("/notes")
@RequiredArgsConstructor
public class NoteController {

    private final NoteService noteService;
    private final CategoryService categoryService;
    private final TagService tagService;
    private final AiNoteAssistantService aiNoteAssistantService;

    @GetMapping
    public String listNotes(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            Model model
    ) {
        model.addAttribute("notes", noteService.searchNotes(query, categoryId, tagId));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("tags", tagService.findAll());
        model.addAttribute("query", query);
        model.addAttribute("selectedCategoryId", categoryId);
        model.addAttribute("selectedTagId", tagId);
        return "notes/list";
    }

    @GetMapping("/search")
    public String searchAlias(
            @RequestParam(required = false) String query,
            @RequestParam(required = false) Long categoryId,
            @RequestParam(required = false) Long tagId,
            Model model
    ) {
        return listNotes(query, categoryId, tagId, model);
    }

    @GetMapping("/new")
    public String showCreateForm(Model model) {
        model.addAttribute("noteForm", new NoteForm());
        model.addAttribute("formMode", "create");
        addReferenceData(model);
        return "notes/form";
    }

    @PostMapping
    public String createNote(
            @Valid @ModelAttribute("noteForm") NoteForm noteForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        if (bindingResult.hasErrors()) {
            model.addAttribute("formMode", "create");
            addReferenceData(model);
            return "notes/form";
        }

        Note savedNote = noteService.createNote(noteForm);
        redirectAttributes.addFlashAttribute("successMessage", "Note created successfully.");
        return "redirect:/notes/" + savedNote.getId();
    }

    @GetMapping("/{id}")
    public String viewNote(@PathVariable Long id, Model model) {
        Note note = noteService.getNoteById(id);
        prepareDetailsModel(note, model);
        return "notes/details";
    }

    @GetMapping("/{id}/edit")
    public String showEditForm(@PathVariable Long id, Model model) {
        Note note = noteService.getNoteById(id);
        model.addAttribute("note", note);
        model.addAttribute("noteForm", NoteForm.fromEntity(note));
        model.addAttribute("formMode", "edit");
        addReferenceData(model);
        return "notes/form";
    }

    @PostMapping("/{id}/update")
    public String updateNote(
            @PathVariable Long id,
            @Valid @ModelAttribute("noteForm") NoteForm noteForm,
            BindingResult bindingResult,
            Model model,
            RedirectAttributes redirectAttributes
    ) {
        Note note = noteService.getNoteById(id);

        if (bindingResult.hasErrors()) {
            model.addAttribute("note", note);
            model.addAttribute("formMode", "edit");
            addReferenceData(model);
            return "notes/form";
        }

        noteService.updateNote(id, noteForm);
        redirectAttributes.addFlashAttribute("successMessage", "Note updated successfully.");
        return "redirect:/notes/" + id;
    }

    @PostMapping("/{id}/delete")
    public String deleteNote(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        noteService.deleteNote(id);
        redirectAttributes.addFlashAttribute("successMessage", "Note deleted successfully.");
        return "redirect:/notes";
    }

    @PostMapping("/{id}/ai/summary")
    public String summarizeNote(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.getNoteById(id);
            prepareDetailsModel(note, model);
            model.addAttribute("aiTitle", "Summary");
            model.addAttribute("aiText", aiNoteAssistantService.summarizeNote(note.getContent()));
            return "notes/details";
        } catch (AiFeatureException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/notes/" + id;
        }
    }

    @PostMapping("/{id}/ai/key-points")
    public String extractKeyPoints(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.getNoteById(id);
            prepareDetailsModel(note, model);
            model.addAttribute("aiTitle", "Key points");
            model.addAttribute("aiItems", aiNoteAssistantService.extractKeyPoints(note.getContent()));
            return "notes/details";
        } catch (AiFeatureException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/notes/" + id;
        }
    }

    @PostMapping("/{id}/ai/quiz")
    public String generateQuiz(@PathVariable Long id, Model model, RedirectAttributes redirectAttributes) {
        try {
            Note note = noteService.getNoteById(id);
            prepareDetailsModel(note, model);
            List<String> questions = aiNoteAssistantService.generateQuizQuestions(note.getContent());
            model.addAttribute("aiTitle", "Quiz questions");
            model.addAttribute("aiItems", questions);
            return "notes/details";
        } catch (AiFeatureException ex) {
            redirectAttributes.addFlashAttribute("errorMessage", ex.getMessage());
            return "redirect:/notes/" + id;
        }
    }

    private void prepareDetailsModel(Note note, Model model) {
        model.addAttribute("note", note);
        model.addAttribute("aiEnabled", aiNoteAssistantService.isConfigured());
        model.addAttribute("aiHint", aiNoteAssistantService.getConfigurationHint());
    }

    private void addReferenceData(Model model) {
        model.addAttribute("categories", categoryService.findAll());
    }
}
