package org.shieldwork.chatmmbackend.service;

import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

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
}