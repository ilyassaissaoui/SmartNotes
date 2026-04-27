package com.example.smartnotes.repository;

import com.example.smartnotes.model.Note;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface NoteRepository extends JpaRepository<Note, Long> {

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    List<Note> findAll();

    @Override
    @EntityGraph(attributePaths = {"category", "tags"})
    Optional<Note> findById(Long id);

    @EntityGraph(attributePaths = {"category", "tags"})
    List<Note> findTop5ByOrderByUpdatedAtDesc();

    @Query("""
            select distinct n
            from Note n
            where
                (:query is null or
                 lower(n.title) like lower(concat('%', :query, '%')) or
                 lower(n.content) like lower(concat('%', :query, '%')))
            and (:categoryId is null or n.category.id = :categoryId)
            and (:tagId is null or exists (
                select 1 from n.tags t where t.id = :tagId
            ))
            order by n.updatedAt desc
            """)
    @EntityGraph(attributePaths = {"category", "tags"})
    List<Note> search(@Param("query") String query,
                      @Param("categoryId") Long categoryId,
                      @Param("tagId") Long tagId);

    @Modifying
    @Query(value = "delete from note_tags where note_id = :noteId", nativeQuery = true)
    void deleteTagLinksByNoteId(@Param("noteId") Long noteId);
}
