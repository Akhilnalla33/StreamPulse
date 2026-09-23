package com.streampulse.auth.controller;

import com.streampulse.auth.dto.LoginRequest;
import com.streampulse.auth.dto.RefreshRequest;
import com.streampulse.auth.dto.RegisterTenantRequest;
import com.streampulse.auth.dto.TokenResponse;
import com.streampulse.auth.service.AuthService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/auth")
public class AuthController {

    private final AuthService authService;

    public AuthController(AuthService authService) {
        this.authService = authService;
    }

    @PostMapping("/register-tenant")
    public ResponseEntity<TokenResponse> registerTenant(@Valid @RequestBody RegisterTenantRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED).body(authService.registerTenant(request));
    }

    @PostMapping("/login")
    public ResponseEntity<TokenResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenResponse> refresh(@Valid @RequestBody RefreshRequest request) {
        return ResponseEntity.ok(authService.refresh(request));
    }
}
