package org.shieldwork.chatmmbackend.controller;

import org.springframework.security.core.Authentication;
import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.LogoutRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.dto.response.AuthResponse;
import org.shieldwork.chatmmbackend.dto.response.SaltResponse;
import org.shieldwork.chatmmbackend.dto.response.TokenRefreshResponse;
import org.shieldwork.chatmmbackend.service.AuthService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<Void> register(@Valid @RequestBody RegisterRequest request) {
        authService.registerUser(request);
        // Some message or metadata could be added in the future
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@Valid @RequestBody LoginRequest request) {
        return ResponseEntity.ok(authService.login(request));
    }

    @PostMapping("/refresh")
    public ResponseEntity<TokenRefreshResponse> refreshToken(@RequestBody RefreshTokenRequest request) {
        return ResponseEntity.ok(authService.refreshToken(request));
    }

    @GetMapping("/salt")
    public ResponseEntity<SaltResponse> getSalt(@RequestParam String email) {
        return ResponseEntity.ok(authService.getFrontSalt(email));
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@Valid @RequestBody LogoutRequest request, Authentication authentication) {
        authService.logout(request.getDeviceId(), authentication.getName());
        return ResponseEntity.noContent().build();
    }

    // just revoke
    // change password
}
