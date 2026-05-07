package org.shieldwork.chatmmbackend.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.dto.response.AuthResponse;
import org.shieldwork.chatmmbackend.dto.response.TokenRefreshResponse;
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

import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService jwtService;
    @Mock private RefreshTokenRepository refreshTokenRepository;

    @InjectMocks
    private AuthService authService;

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(authService, "refreshTokenExpirationMs", 2592000000L);
    }


    @Test
    void registerUser_ShouldSaveNewUser_WhenEmailIsNotTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("newuser@example.com");
        request.setPassword("plainPassword");
        request.setName("John Doe");
        request.setFrontSalt("salt");
        request.setPublicKey("pubKey");
        request.setEncryptedPrivateKey("privKey");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(false);
        when(passwordEncoder.encode(request.getPassword())).thenReturn("hashedPassword");

        authService.registerUser(request);

        ArgumentCaptor<User> userCaptor = ArgumentCaptor.forClass(User.class);
        verify(userRepository, times(1)).save(userCaptor.capture());

        User savedUser = userCaptor.getValue();
        assertEquals("newuser@example.com", savedUser.getEmail(), "Email should match request");
        assertEquals("hashedPassword", savedUser.getPassword(), "Password should be hashed");
    }

    @Test
    void registerUser_ShouldThrowException_WhenEmailIsAlreadyTaken() {
        RegisterRequest request = new RegisterRequest();
        request.setEmail("existing@example.com");

        when(userRepository.existsByEmail(request.getEmail())).thenReturn(true);

        UserAlreadyExistsException exception = assertThrows(
                UserAlreadyExistsException.class,
                () -> authService.registerUser(request),
                "Should throw exception when email is taken"
        );

        assertEquals("An account with this address already exists.", exception.getMessage());
        verify(userRepository, never()).save(any(User.class));
    }

    @Test
    void login_ShouldReturnAuthResponse_WhenCredentialsAreValid() {
        LoginRequest request = new LoginRequest();
        request.setEmail("user@example.com");
        request.setPassword("password123");
        request.setDeviceId("device-123");

        User user = User.builder()
                .id(1L)
                .email("user@example.com")
                .name("John Doe")
                .encryptedPrivateKey("secretKey")
                .build();

        when(userRepository.findByEmail(request.getEmail())).thenReturn(Optional.of(user));
        when(refreshTokenRepository.existsByUserAndDeviceId(user, request.getDeviceId())).thenReturn(false);
        when(jwtService.generateToken(user)).thenReturn("mocked-jwt-token");

        AuthResponse response = authService.login(request);

        assertNotNull(response, "AuthResponse should not be null");
        assertEquals("mocked-jwt-token", response.getAccessToken());
        assertEquals("secretKey", response.getEncryptedPrivateKey(), "Should return private key for new device");

        verify(authenticationManager).authenticate(any(UsernamePasswordAuthenticationToken.class));
        verify(refreshTokenRepository).deleteByUserAndDeviceId(user, "device-123");
        verify(refreshTokenRepository).save(any(RefreshToken.class));
    }

    @Test
    void refreshToken_ShouldThrowException_WhenTokenIsExpired() {
        RefreshTokenRequest request = new RefreshTokenRequest();
        request.setRefreshToken("expired-refresh-token");

        RefreshToken expiredToken = RefreshToken.builder()
                .token("expired-refresh-token")
                .expiryDate(Instant.now().minusSeconds(3600))
                .user(new User())
                .build();

        when(refreshTokenRepository.findByToken(request.getRefreshToken())).thenReturn(Optional.of(expiredToken));

        TokenRefreshException exception = assertThrows(
                TokenRefreshException.class,
                () -> authService.refreshToken(request)
        );

        assertEquals("Refresh token has expired.", exception.getMessage());
        verify(refreshTokenRepository).delete(expiredToken);
        verify(jwtService, never()).generateToken(any());
    }
}