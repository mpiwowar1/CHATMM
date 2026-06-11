package org.shieldwork.chatmmbackend.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.response.UserAutocompleteResponse;
import org.shieldwork.chatmmbackend.dto.response.UserResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.springframework.data.domain.PageRequest;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock UserRepository userRepository;
    @InjectMocks UserService userService;


    @Test
    void findUserByEmail_returnsResponse_whenFound() {
        User user = buildUser(1L, "alice@example.com", "Alice");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        UserResponse response = userService.findUserByEmail("alice@example.com");

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getName()).isEqualTo("Alice");
        assertThat(response.getPublicKey()).isEqualTo("pubkey");
    }

    @Test
    void findUserByEmail_throws_whenNotFound() {
        when(userRepository.findByEmail(anyString())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> userService.findUserByEmail("nobody@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void autocomplete_returnsResults_forValidQuery() {
        List<User> users = List.of(
                buildUser(1L, "alice@example.com", "Alice"),
                buildUser(2L, "alan@example.com", "Alan")
        );
        when(userRepository.findByEmailContainingIgnoreCase(eq("al"), any())).thenReturn(users);

        List<UserAutocompleteResponse> result = userService.autocompleteUsers("al", 10);

        assertThat(result).hasSize(2);
        assertThat(result).extracting(UserAutocompleteResponse::getEmail)
                .containsExactlyInAnyOrder("alice@example.com", "alan@example.com");
    }

    @Test
    void autocomplete_returnsEmpty_forNullQuery() {
        List<UserAutocompleteResponse> result = userService.autocompleteUsers(null, 10);
        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void autocomplete_returnsEmpty_forBlankQuery() {
        List<UserAutocompleteResponse> result = userService.autocompleteUsers("   ", 10);
        assertThat(result).isEmpty();
        verifyNoInteractions(userRepository);
    }

    @Test
    void autocomplete_clampsLimit_toMax20() {
        when(userRepository.findByEmailContainingIgnoreCase(eq("test"), any())).thenReturn(List.of());

        userService.autocompleteUsers("test", 999);

        verify(userRepository).findByEmailContainingIgnoreCase(eq("test"), eq(PageRequest.of(0, 20)));
    }

    @Test
    void autocomplete_clampsLimit_toMin1() {
        when(userRepository.findByEmailContainingIgnoreCase(eq("test"), any())).thenReturn(List.of());

        userService.autocompleteUsers("test", 0);

        verify(userRepository).findByEmailContainingIgnoreCase(eq("test"), eq(PageRequest.of(0, 1)));
    }


    private User buildUser(Long id, String email, String name) {
        return User.builder()
                .id(id)
                .email(email)
                .name(name)
                .password("pass")
                .frontSalt("salt")
                .publicKey("pubkey")
                .encryptedPrivateKey("encpriv")
                .build();
    }
}
