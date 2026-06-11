package org.shieldwork.chatmmbackend.controller;

import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.BaseIntegrationTest;
import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.LogoutRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.model.RefreshToken;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class AuthControllerIT extends BaseIntegrationTest {

    @BeforeEach
    void clean() {
        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
    }


    @Test
    void register_returns201_onValidRequest() throws Exception {
        RegisterRequest req = buildRegisterRequest("new@example.com", "New User");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(userRepository.existsByEmail("new@example.com")).isTrue();
    }

    @Test
    void register_returns422_onInvalidEmail() throws Exception {
        RegisterRequest req = buildRegisterRequest("not-an-email", "User");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    void register_returns409_whenEmailAlreadyExists() throws Exception {
        createUser("exists@example.com", "Existing");

        RegisterRequest req = buildRegisterRequest("exists@example.com", "Duplicate");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void register_returns422_whenNameTooShort() throws Exception {
        RegisterRequest req = buildRegisterRequest("short@example.com", "A");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity());
    }


    @Test
    void login_returns200_andTokens_onValidCredentials() throws Exception {
        createUser("alice@example.com", "Alice");

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("Password1!");
        req.setDeviceId("device-1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.email").value("alice@example.com"));
    }

    @Test
    void login_returns401_onWrongPassword() throws Exception {
        createUser("alice@example.com", "Alice");

        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("WrongPassword!");
        req.setDeviceId("device-1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void login_returns401_onUnknownEmail() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("ghost@example.com");
        req.setPassword("Password1!");
        req.setDeviceId("device-1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void refresh_returns200_andNewAccessToken() throws Exception {
        User user = createUser("alice@example.com", "Alice");

        RefreshToken rt = RefreshToken.builder()
                .token("valid-rt")
                .user(user)
                .deviceId("device-1")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
        refreshTokenRepository.save(rt);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("valid-rt");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty());
    }

    @Test
    void refresh_returns403_whenTokenExpired() throws Exception {
        User user = createUser("alice@example.com", "Alice");

        RefreshToken rt = RefreshToken.builder()
                .token("expired-rt")
                .user(user)
                .deviceId("device-1")
                .expiryDate(Instant.now().minusSeconds(1))
                .build();
        refreshTokenRepository.save(rt);

        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("expired-rt");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }

    @Test
    void refresh_returns403_whenTokenNotFound() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("nonexistent");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden());
    }


    @Test
    void getSalt_returns200_withSalt() throws Exception {
        createUser("alice@example.com", "Alice");

        mockMvc.perform(get("/auth/salt").param("email", "alice@example.com"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.frontSalt").value("test-salt"));
    }

    @Test
    void getSalt_returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(get("/auth/salt").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound());
    }

    @Test
    void getSalt_returns400_whenEmailParamMissing() throws Exception {
        mockMvc.perform(get("/auth/salt"))
                .andExpect(status().isBadRequest());
    }


    @Test
    void logout_returns204_andDeletesToken() throws Exception {
        User user = createUser("alice@example.com", "Alice");

        RefreshToken rt = RefreshToken.builder()
                .token("rt-to-delete")
                .user(user)
                .deviceId("device-1")
                .expiryDate(Instant.now().plusSeconds(3600))
                .build();
        refreshTokenRepository.save(rt);

        LogoutRequest req = new LogoutRequest();
        req.setDeviceId("device-1");

        mockMvc.perform(post("/auth/logout")
                        .header("Authorization", bearerToken(user))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isNoContent());

        assertThat(refreshTokenRepository.findByToken("rt-to-delete")).isEmpty();
    }

    @Test
    void logout_requiresAuthentication() throws Exception {
        
        LogoutRequest req = new LogoutRequest();
        req.setDeviceId("device-1");

        int status = mockMvc.perform(post("/auth/logout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andReturn().getResponse().getStatus();

        assertThat(status).isNotIn(200, 201, 204);
    }


    private RegisterRequest buildRegisterRequest(String email, String name) {
        RegisterRequest req = new RegisterRequest();
        req.setEmail(email);
        req.setName(name);
        req.setPassword("Password1!");
        req.setFrontSalt("salt");
        req.setPublicKey("pubkey");
        req.setEncryptedPrivateKey("encpriv");
        return req;
    }
}
