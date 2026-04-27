package com.example.smartnotes.service;

import com.example.smartnotes.model.Tag;
import com.example.smartnotes.repository.TagRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TagService {

    private final TagRepository tagRepository;

    public List<Tag> findAll() {
        return tagRepository.findAll()
                .stream()
                .sorted((a, b) -> a.getName().compareToIgnoreCase(b.getName()))
                .toList();
    }

    @Transactional
    public Set<Tag> resolveTags(String commaSeparatedTags) {
        if (!StringUtils.hasText(commaSeparatedTags)) {
            return new LinkedHashSet<>();
        }

        return Arrays.stream(commaSeparatedTags.split(","))
                .map(String::trim)
                .filter(StringUtils::hasText)
                .map(tag -> tag.toLowerCase(Locale.ROOT))
                .distinct()
                .map(this::getOrCreate)
                .collect(LinkedHashSet::new, Set::add, Set::addAll);
    }

    @Transactional
    public Tag getOrCreate(String tagName) {
        return tagRepository.findByNameIgnoreCase(tagName)
                .orElseGet(() -> tagRepository.save(new Tag(tagName)));
    }
}
