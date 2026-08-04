package com.transitops.backend.controller;

import com.transitops.backend.dto.auth.*;
import com.transitops.backend.entity.User;
import com.transitops.backend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@Valid @RequestBody RegisterRequest request) {
        return ResponseEntity.ok(authService.register(request));
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }

    @PostMapping("/logout")
    public ResponseEntity<Map<String, String>> logout(@AuthenticationPrincipal User user) {
        if (user != null) {
            authService.logout(user);
        }
        return ResponseEntity.ok(Map.of("message", "Logged out"));
    }

    @GetMapping("/me")
    public ResponseEntity<AuthResponse.UserDto> me(@AuthenticationPrincipal User user) {
        return ResponseEntity.ok(authService.me(user));
    }
}
