package com.example.smartnotes.controller.api;

import com.example.smartnotes.dto.api.AuthResponse;
import com.example.smartnotes.dto.api.LoginRequest;
import com.example.smartnotes.dto.api.RegisterRequest;
import com.example.smartnotes.dto.api.UserResponse;
import com.example.smartnotes.security.CurrentUserService;
import com.example.smartnotes.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class ApiAuthController {

    private final AuthService authService;
    private final CurrentUserService currentUserService;

    @PostMapping("/register")
    public AuthResponse register(@Valid @RequestBody RegisterRequest request) {
        return authService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return authService.login(request);
    }

    @GetMapping("/me")
    public UserResponse me() {
        return UserResponse.fromEntity(currentUserService.getCurrentUser());
    }
}
