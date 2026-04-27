package com.example.smartnotes.exception;

import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.resource.NoResourceFoundException;

@ControllerAdvice(annotations = Controller.class)
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(NoteNotFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleNoteNotFound(NoteNotFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Note not found");
        model.addAttribute("errorMessage", ex.getMessage());
        model.addAttribute("backHref", "/notes");
        model.addAttribute("backLabel", "Back to notes");
        return "error";
    }

    @ExceptionHandler(DataAccessException.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleDatabaseError(DataAccessException ex, Model model, HttpServletRequest request) {
        log.error("Database error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        model.addAttribute("errorTitle", "Database error");
        model.addAttribute("errorMessage", "A database error occurred while processing your request.");
        model.addAttribute("backHref", "/");
        model.addAttribute("backLabel", "Back to dashboard");
        return "error";
    }

    @ExceptionHandler(NoResourceFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public String handleResourceNotFound(NoResourceFoundException ex, Model model) {
        model.addAttribute("errorTitle", "Not found");
        model.addAttribute("errorMessage", "The requested resource was not found.");
        model.addAttribute("backHref", "/notes");
        model.addAttribute("backLabel", "Back to notes");
        return "error";
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public String handleGenericError(Exception ex, Model model, HttpServletRequest request) {
        log.error("Unexpected application error on {} {}", request.getMethod(), request.getRequestURI(), ex);
        model.addAttribute("errorTitle", "Unexpected error");
        model.addAttribute("errorMessage", "Something went wrong. Please try again.");
        model.addAttribute("backHref", "/");
        model.addAttribute("backLabel", "Back to dashboard");
        return "error";
    }
}
