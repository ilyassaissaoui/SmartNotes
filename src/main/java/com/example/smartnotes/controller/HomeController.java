package com.example.smartnotes.controller;

import com.example.smartnotes.service.NoteService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
@RequiredArgsConstructor
public class HomeController {

    private final NoteService noteService;

    @GetMapping("/")
    public String home(Model model) {
        model.addAttribute("noteCount", noteService.countNotes());
        model.addAttribute("latestNotes", noteService.findLatestNotes());
        return "home";
    }
}
