package com.example.smartnotes.controller.api;

import com.example.smartnotes.dto.api.CategoryResponse;
import com.example.smartnotes.dto.api.TagResponse;
import com.example.smartnotes.service.CategoryService;
import com.example.smartnotes.service.TagService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class ApiReferenceController {

    private final CategoryService categoryService;
    private final TagService tagService;

    @GetMapping("/categories")
    public List<CategoryResponse> categories() {
        return categoryService.findAll()
                .stream()
                .map(CategoryResponse::fromEntity)
                .toList();
    }

    @GetMapping("/tags")
    public List<TagResponse> tags() {
        return tagService.findAll()
                .stream()
                .map(TagResponse::fromEntity)
                .toList();
    }
}
