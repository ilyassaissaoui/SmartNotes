package com.example.smartnotes.config;

import com.example.smartnotes.model.Category;
import com.example.smartnotes.model.Tag;
import com.example.smartnotes.repository.CategoryRepository;
import com.example.smartnotes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final CategoryRepository categoryRepository;
    private final TagRepository tagRepository;

    @Value("${app.seed.categories-and-tags:true}")
    private boolean seedCategoriesAndTags;

    @Override
    public void run(String... args) {
        if (!seedCategoriesAndTags) {
            return;
        }

        saveCategory("Study");
        saveCategory("Work");
        saveCategory("Personal");
        saveCategory("Ideas");

        saveTag("java");
        saveTag("spring");
        saveTag("exam");
        saveTag("project");
        saveTag("productivity");
        saveTag("summary");
        saveTag("docker");
        saveTag("postgres");
    }

    private Category saveCategory(String name) {
        return categoryRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> categoryRepository.save(new Category(name)));
    }

    private Tag saveTag(String name) {
        return tagRepository.findByNameIgnoreCase(name)
                .orElseGet(() -> tagRepository.save(new Tag(name)));
    }
}
