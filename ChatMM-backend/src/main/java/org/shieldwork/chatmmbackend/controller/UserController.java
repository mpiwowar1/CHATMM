package org.shieldwork.chatmmbackend.controller;

import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    // Autocomplete ?
    @GetMapping("/search")
    public ResponseEntity<UserResponse> findUserByEmail(@RequestParam String email) {
        UserResponse response = userService.findUserByEmail(email);
        return ResponseEntity.ok(response);
    }
}