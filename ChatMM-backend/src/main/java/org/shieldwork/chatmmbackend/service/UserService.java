package org.shieldwork.chatmmbackend.service;

import org.shieldwork.chatmmbackend.dto.response.UserAutocompleteResponse;
import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;

    @Transactional(readOnly = true)
    public UserResponse findUserByEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        return UserResponse.builder()
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .publicKey(user.getPublicKey())
                .build();
    }

    @Transactional(readOnly = true)
    public List<UserAutocompleteResponse> autocompleteUsers(String query, int limit) {
        if (query == null || query.trim().isEmpty()) {
            return List.of();
        }

        int finalLimit = Math.max(1, Math.min(limit, 20));

        Pageable pageable = PageRequest.of(0, finalLimit);

        List<User> users = userRepository.findByEmailContainingIgnoreCase(query, pageable);

        return users.stream()
                .map(user -> UserAutocompleteResponse.builder()
                        .email(user.getEmail())
                        .name(user.getName())
                        .build())
                .toList();
    }
}