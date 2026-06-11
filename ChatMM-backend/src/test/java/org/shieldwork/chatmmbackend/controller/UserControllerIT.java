package org.shieldwork.chatmmbackend.controller;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.BaseIntegrationTest;
import org.shieldwork.chatmmbackend.model.User;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class UserControllerIT extends BaseIntegrationTest {

    private User alice;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        alice = createUser("alice@example.com", "Alice");
        createUser("albert@example.com", "Albert");
        createUser("bob@example.com", "Bob");
    }

    

    @Test
    void findByEmail_returns200_withUserData() throws Exception {
        mockMvc.perform(get("/users/search")
                        .param("email", "alice@example.com")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"))
                .andExpect(jsonPath("$.publicKey").value("public-key-alice@example.com"));
    }

    @Test
    void findByEmail_returns404_whenNotFound() throws Exception {
        mockMvc.perform(get("/users/search")
                        .param("email", "nobody@example.com")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void findByEmail_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/users/search").param("email", "alice@example.com"))
                .andExpect(status().isUnauthorized());
    }

    

    @Test
    void getMe_returns200_withCurrentUserData() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.email").value("alice@example.com"))
                .andExpect(jsonPath("$.name").value("Alice"));
    }

    @Test
    void getMe_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

   

    @Test
    void autocomplete_returnsMatchingUsers() throws Exception {
        mockMvc.perform(get("/users/autocomplete")
                        .param("query", "al")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$.length()").value(2)); 
    }

    @Test
    void autocomplete_returnsEmpty_forNoMatch() throws Exception {
        mockMvc.perform(get("/users/autocomplete")
                        .param("query", "zzz")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isEmpty());
    }

    @Test
    void autocomplete_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/users/autocomplete").param("query", "al"))
                .andExpect(status().isUnauthorized());
    }
}
