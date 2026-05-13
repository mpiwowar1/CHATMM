package org.shieldwork.chatmmbackend.service;

import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
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
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final AuthenticationManager authenticationManager;
    private final JwtService jwtService;
    private final RefreshTokenRepository refreshTokenRepository;

    @Value("${application.security.jwt.refresh-token.expiration}")
    private long refreshTokenExpirationMs;

    @Transactional
    public void registerUser(RegisterRequest request) {
        if (userRepository.existsByEmail(request.getEmail())) {
            throw new UserAlreadyExistsException("An account with this address already exists.");
        }

        String doubleHashedPassword = passwordEncoder.encode(request.getPassword());

        User newUser = User.builder()
                .email(request.getEmail())
                .name(request.getName())
                .password(doubleHashedPassword)
                .frontSalt(request.getFrontSalt())
                .publicKey(request.getPublicKey())
                .encryptedPrivateKey(request.getEncryptedPrivateKey())
                .build();

        userRepository.save(newUser);
    }

    @Transactional
    public AuthResponse login(LoginRequest request) {
        authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        User user = userRepository.findByEmail(request.getEmail())
                .orElseThrow(() -> new ResourceNotFoundException("User not found.")); // with this email

        boolean isNewDevice = !refreshTokenRepository.existsByUserAndDeviceId(user, request.getDeviceId());

        String jwtToken = jwtService.generateToken(user);

        refreshTokenRepository.deleteByUserAndDeviceId(user, request.getDeviceId());

        String refreshTokenStr = UUID.randomUUID().toString();
        RefreshToken refreshToken = RefreshToken.builder()
                .token(refreshTokenStr)
                .user(user)
                .deviceId(request.getDeviceId())
                .expiryDate(Instant.now().plusMillis(refreshTokenExpirationMs))
                .build();

        refreshTokenRepository.save(refreshToken);

        return AuthResponse.builder()
                .accessToken(jwtToken)
                .refreshToken(refreshTokenStr)
                .id(user.getId())
                .email(user.getEmail())
                .name(user.getName())
                .encryptedPrivateKey(isNewDevice ? user.getEncryptedPrivateKey() : null)
                .build();
    }

    @Transactional
    public TokenRefreshResponse refreshToken(RefreshTokenRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        RefreshToken refreshTokenEntity = refreshTokenRepository.findByToken(requestRefreshToken)
                .orElseThrow(() -> new TokenRefreshException("Refresh token not found."));


        if (refreshTokenEntity.getExpiryDate().isBefore(Instant.now())) {
            refreshTokenRepository.delete(refreshTokenEntity);
            throw new TokenRefreshException("Refresh token has expired.");
        }

        User user = refreshTokenEntity.getUser();
        String newAccessToken = jwtService.generateToken(user);

        return new TokenRefreshResponse(newAccessToken, requestRefreshToken);
    }

    @Transactional(readOnly = true)
    public SaltResponse getFrontSalt(String email) {
        return userRepository.findByEmail(email)
                .map(user -> new SaltResponse(user.getFrontSalt()))
                .orElseThrow(() -> new ResourceNotFoundException("Salt not found."));
    }

    @Transactional
    public void logout(String deviceId, String userEmail) {
        User user = userRepository.findByEmail(userEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        refreshTokenRepository.deleteByUserAndDeviceId(user, deviceId);
    }
}
