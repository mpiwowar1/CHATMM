package org.shieldwork.chatmmbackend.security;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.BaseIntegrationTest;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.test.util.ReflectionTestUtils;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;


@Transactional
class JwtAuthFilterIT extends BaseIntegrationTest {

    private User alice;

    @BeforeEach
    void setup() {
        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        messageRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();
        alice = createUser("alice@example.com", "Alice");
    }

    

    @Test
    void validToken_allowsAccessToProtectedEndpoint() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk());
    }

    

    @Test
    void noToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void malformedToken_returns401() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer this.is.not.a.valid.jwt"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void tokenWithoutBearerPrefix_returns401() throws Exception {
        String rawToken = jwtService.generateToken(alice);
        mockMvc.perform(get("/users/me")
                        .header("Authorization", rawToken))
                .andExpect(status().isUnauthorized());
    }

    @Test
    void emptyAuthorizationHeader_returns401() throws Exception {
        mockMvc.perform(get("/users/me")
                        .header("Authorization", ""))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void expiredToken_returns401() throws Exception {
        JwtService shortLivedJwt = new JwtService();
        ReflectionTestUtils.setField(shortLivedJwt, "secretKey",
                "eed9739e4b81a345558172e4b526b7589dd2db76a92ad4a8fd215cd89552432a");
        ReflectionTestUtils.setField(shortLivedJwt, "jwtExpiration", -1L);

        String expiredToken = shortLivedJwt.generateToken(alice);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", "Bearer " + expiredToken))
                .andExpect(status().isUnauthorized());
    }


    @Test
    void authEndpoints_accessibleWithoutToken() throws Exception {
        mockMvc.perform(get("/auth/salt").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound());
    }


    @Test
    void tokenForDeletedUser_returns401() throws Exception {
        String token = bearerToken(alice);

        refreshTokenRepository.deleteAll();
        participantRepository.deleteAll();
        userRepository.delete(alice);

        mockMvc.perform(get("/users/me")
                        .header("Authorization", token))
                .andExpect(status().isUnauthorized());
    }
}
