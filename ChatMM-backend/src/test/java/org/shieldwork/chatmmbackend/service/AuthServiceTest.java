package org.shieldwork.chatmmbackend.service;

import java.time.Instant;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.dto.response.AuthResponse;
import org.shieldwork.chatmmbackend.dto.response.SaltResponse;
import org.shieldwork.chatmmbackend.dto.response.TokenRefreshResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.exception.TokenRefreshException;
import org.shieldwork.chatmmbackend.exception.UserAlreadyExistsException;
import org.shieldwork.chatmmbackend.model.RefreshToken;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.RefreshTokenRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.shieldwork.chatmmbackend.security.JwtService;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock AuthenticationManager authenticationManager;
    @Mock JwtService jwtService;
    @Mock RefreshTokenRepository refreshTokenRepository;

    @InjectMocks AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 2_592_000_000L);
    }


    @Test
    void register_savesUser_whenEmailNotTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("new@example.com");
        req.setName("New User");
        req.setPassword("hashed-on-front");
        req.setFrontSalt("salt");
        req.setPublicKey("pubkey");
        req.setEncryptedPrivateKey("encpriv");

        when(userRepository.existsByEmail("new@example.com")).thenReturn(false);
        when(passwordEncoder.encode(any())).thenReturn("bcrypted");

        authService.registerUser(req);

        verify(userRepository).save(argThat(u ->
                u.getEmail().equals("new@example.com") &&
                u.getName().equals("New User") &&
                u.getPassword().equals("bcrypted")
        ));
    }

    @Test
    void register_throws_whenEmailAlreadyTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setEmail("taken@example.com");
        req.setName("X");
        req.setPassword("p");
        req.setFrontSalt("s");
        req.setPublicKey("k");
        req.setEncryptedPrivateKey("e");

        when(userRepository.existsByEmail("taken@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.registerUser(req))
                .isInstanceOf(UserAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
    }


    @Test
    void login_returnsAuthResponse_onValidCredentials() {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("pass");
        req.setDeviceId("device-1");

        User user = buildUser(1L, "alice@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));
        when(jwtService.generateToken(user)).thenReturn("jwt-token");

        AuthResponse response = authService.login(req);

        assertThat(response.getAccessToken()).isEqualTo("jwt-token");
        assertThat(response.getEmail()).isEqualTo("alice@example.com");
        assertThat(response.getRefreshToken()).isNotBlank();

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).deleteByUserAndDeviceId(user, "device-1");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void login_throws_whenUserNotFound() {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("pass");
        req.setDeviceId("device-1");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.login(req))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void refreshToken_returnsNewAccessToken_whenTokenValid() {
        User user = buildUser(1L, "alice@example.com");

        RefreshToken rt = RefreshToken.builder()
                .token("valid-refresh")
                .user(user)
                .deviceId("device-1")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("valid-refresh");

        when(refreshTokenRepository.findByToken("valid-refresh")).thenReturn(Optional.of(rt));
        when(jwtService.generateToken(user)).thenReturn("new-jwt");

        TokenRefreshResponse response = authService.refreshToken(req);

        assertThat(response.getAccessToken()).isEqualTo("new-jwt");
        assertThat(response.getRefreshToken()).isEqualTo("valid-refresh");
    }

    @Test
    void refreshToken_throws_whenTokenNotFound() {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("unknown");

        when(refreshTokenRepository.findByToken("unknown")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("not found");
    }

    @Test
    void refreshToken_throws_andDeletesToken_whenExpired() {
        User user = buildUser(1L, "alice@example.com");

        RefreshToken expired = RefreshToken.builder()
                .token("expired-token")
                .user(user)
                .deviceId("device-1")
                .expiryDate(Instant.now().minusSeconds(1))
                .build();

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("expired-token");

        when(refreshTokenRepository.findByToken("expired-token")).thenReturn(Optional.of(expired));

        assertThatThrownBy(() -> authService.refreshToken(req))
                .isInstanceOf(TokenRefreshException.class)
                .hasMessageContaining("expired");

        verify(refreshTokenRepository).delete(expired);
    }


    @Test
    void getFrontSalt_returnsSalt_whenUserExists() {
        User user = buildUser(1L, "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        SaltResponse response = authService.getFrontSalt("alice@example.com");

        assertThat(response.getFrontSalt()).isEqualTo("front-salt");
    }

    @Test
    void getFrontSalt_throws_whenUserNotFound() {
        when(userRepository.findByEmail("nobody@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.getFrontSalt("nobody@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void logout_deletesRefreshToken() {
        User user = buildUser(1L, "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(user));

        authService.logout("device-1", "alice@example.com");

        verify(refreshTokenRepository).deleteByUserAndDeviceId(user, "device-1");
    }

    @Test
    void logout_throws_whenUserNotFound() {
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> authService.logout("device-1", "ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    private User buildUser(Long id, String email) {
        return User.builder()
                .id(id)
                .email(email)
                .name("Alice")
                .password("bcrypted")
                .frontSalt("front-salt")
                .publicKey("pubkey")
                .encryptedPrivateKey("encpriv")
                .build();
    }
}
