package com.dotlabs.moneytransfer.controller;

import com.dotlabs.moneytransfer.dto.request.LoginRequest;
import com.dotlabs.moneytransfer.dto.request.RegisterRequest;
import com.dotlabs.moneytransfer.dto.response.ApiResponse;
import com.dotlabs.moneytransfer.dto.response.AuthResponse;
import com.dotlabs.moneytransfer.entity.User;
import com.dotlabs.moneytransfer.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Tag(name = "Authentication", description = "Endpoints for user registration, login, and JWT token issuance")
public class AuthController {

    private final AuthService authService;

    @PostMapping("/login")
    @Operation(summary = "Authenticate user and get JWT token", description = "Validates username and password, returning a Bearer JWT token.")
    public ResponseEntity<ApiResponse<AuthResponse>> login(@Valid @RequestBody LoginRequest request) {
        AuthResponse response = authService.login(request);
        return ResponseEntity.ok(ApiResponse.success(response, "Login successful"));
    }

    @PostMapping("/register")
    @Operation(summary = "Register a new user", description = "Creates a new user account with an auto-assigned bank account and opening balance.")
    public ResponseEntity<ApiResponse<AuthResponse>> register(@Valid @RequestBody RegisterRequest request) {
        AuthResponse response = authService.register(request);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success(response, "User registered successfully"));
    }

    @GetMapping("/me")
    @Operation(summary = "Get current authenticated user profile", description = "Returns profile and account details for the currently logged-in user.")
    public ResponseEntity<ApiResponse<User>> getCurrentUser(@AuthenticationPrincipal User user) {
        if (user == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(ApiResponse.error("Not authenticated"));
        }
        return ResponseEntity.ok(ApiResponse.success(user, "User profile retrieved"));
    }
}
