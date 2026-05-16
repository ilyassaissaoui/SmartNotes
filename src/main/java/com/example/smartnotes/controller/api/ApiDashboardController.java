package com.example.smartnotes.controller.api;

import com.example.smartnotes.dto.api.DashboardResponse;
import com.example.smartnotes.dto.api.NoteResponse;
import com.example.smartnotes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class ApiDashboardController {

    private final NoteService noteService;

    @GetMapping
    public DashboardResponse dashboard() {
        return new DashboardResponse(
                noteService.countNotes(),
                noteService.findLatestNotes().stream().map(NoteResponse::fromEntity).toList()
        );
    }
}
