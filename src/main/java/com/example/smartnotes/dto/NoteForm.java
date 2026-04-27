package com.example.smartnotes.dto;

import com.example.smartnotes.model.Note;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

import java.util.stream.Collectors;

@Getter
@Setter
public class NoteForm {

    @NotBlank(message = "Title is required.")
    @Size(min = 3, max = 150, message = "Title must be between 3 and 150 characters.")
    private String title;

    @NotBlank(message = "Content is required.")
    @Size(min = 10, message = "Content must contain at least 10 characters.")
    private String content;

    @NotNull(message = "Category is required.")
    private Long categoryId;

    private String tags;

    public static NoteForm fromEntity(Note note) {
        NoteForm form = new NoteForm();
        form.setTitle(note.getTitle());
        form.setContent(note.getContent());
        form.setCategoryId(note.getCategory().getId());
        form.setTags(note.getTags().stream()
                .map(tag -> tag.getName())
                .collect(Collectors.joining(", ")));
        return form;
    }
}
