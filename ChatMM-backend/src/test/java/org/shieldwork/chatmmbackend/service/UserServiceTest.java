package org.shieldwork.chatmmbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.UserRepository;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserService userService;

    @Test
    void findUserByEmail_ShouldReturnUserResponse_WhenUserExists() {
        String email = "test@example.com";
        User mockUser = User.builder()
                .id(1L)
                .email(email)
                .name("John Doe")
                .publicKey("samplePublicKey")
                .build();

        when(userRepository.findByEmail(email)).thenReturn(Optional.of(mockUser));

        UserResponse response = userService.findUserByEmail(email);

        assertNotNull(response, "Response should not be null");
        assertEquals(1L, response.getId(), "User ID should match");
        assertEquals("test@example.com", response.getEmail(), "Email should match");
        assertEquals("John Doe", response.getName(), "Name should match");
        assertEquals("samplePublicKey", response.getPublicKey(), "Public key should match");

        verify(userRepository, times(1)).findByEmail(email);
    }

    @Test
    void findUserByEmail_ShouldThrowResourceNotFoundException_WhenUserDoesNotExist() {
        String email = "unknown@example.com";

        when(userRepository.findByEmail(email)).thenReturn(Optional.empty());

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> userService.findUserByEmail(email),
                "Should throw ResourceNotFoundException when user does not exist"
        );

        assertEquals("User not found.", exception.getMessage());

        verify(userRepository, times(1)).findByEmail(email);
    }
}