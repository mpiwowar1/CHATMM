package org.shieldwork.chatmmbackend.controller;

import org.springframework.security.core.Authentication;
import org.shieldwork.chatmmbackend.dto.response.UserAutocompleteResponse;
import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/search")
    public ResponseEntity<UserResponse> findUserByEmail(@RequestParam String email) {
        UserResponse response = userService.findUserByEmail(email);
        return ResponseEntity.ok(response);
    }

    @GetMapping("/autocomplete")
    public ResponseEntity<List<UserAutocompleteResponse>> autocompleteUsers(
            @RequestParam String query,
            @RequestParam(defaultValue = "5") int limit
    ) {
        List<UserAutocompleteResponse> responses = userService.autocompleteUsers(query, limit);
        return ResponseEntity.ok(responses);
    }

    @GetMapping("/me")
    public ResponseEntity<UserResponse> getCurrentUser(Authentication authentication) {
        String userEmail = authentication.getName();

        UserResponse response = userService.findUserByEmail(userEmail);

        return ResponseEntity.ok(response);
    }
}