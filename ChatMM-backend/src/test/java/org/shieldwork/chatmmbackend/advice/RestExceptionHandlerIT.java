package org.shieldwork.chatmmbackend.advice;

import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.BaseIntegrationTest;
import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.dto.request.LoginRequest;
import org.shieldwork.chatmmbackend.dto.request.RefreshTokenRequest;
import org.shieldwork.chatmmbackend.dto.request.RegisterRequest;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;


@Transactional
class RestExceptionHandlerIT extends BaseIntegrationTest {

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
    void returns404_whenUserNotFound() throws Exception {
        mockMvc.perform(get("/users/search")
                        .param("email", "nobody@example.com")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.title").value("Resource Not Found"));
    }

    @Test
    void returns404_whenSaltRequestedForUnknownEmail() throws Exception {
        mockMvc.perform(get("/auth/salt").param("email", "ghost@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }


    @Test
    void returns409_whenRegisteringDuplicateEmail() throws Exception {
        RegisterRequest req = buildRegisterRequest("alice@example.com", "Alice2");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("User Already Exists"));
    }


    @Test
    void returns409_whenDirectConversationAlreadyExists() throws Exception {
        User bob = createUser("bob@example.com", "Bob");
        createDirectConversation(alice, bob);

        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(alice.getId(), "k1", bob.getId(), "k2"));

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409))
                .andExpect(jsonPath("$.title").value("Conversation Already Exists"));
    }


    @Test
    void returns422_onValidationFailure_invalidEmail() throws Exception {
        RegisterRequest req = buildRegisterRequest("not-valid", "Alice");

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.title").value("Validation Failed"))
                .andExpect(jsonPath("$.detail").isString());
    }

    @Test
    void returns422_onValidationFailure_blankName() throws Exception {
        RegisterRequest req = buildRegisterRequest("valid@example.com", "A"); 

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }


    @Test
    void returns400_whenCreatorNotInParticipants() throws Exception {
        User bob = createUser("bob@example.com", "Bob");
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(bob.getId(), "k1", 999L, "k2")); 

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Bad Request"));
    }


    @Test
    void returns400_onMalformedJson() throws Exception {
        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ this is not valid json }"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Malformed JSON Request"));
    }

    

    @Test
    void returns400_whenRequiredParamMissing() throws Exception {
        mockMvc.perform(get("/auth/salt"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Missing Request Parameter"))
                .andExpect(jsonPath("$.detail").value(org.hamcrest.Matchers.containsString("email")));
    }

   

    @Test
    void returns400_whenPathVariableIsWrongType() throws Exception {
        mockMvc.perform(get("/conversations/not-a-number/keys")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.title").value("Invalid Parameter Type"));
    }

    

    @Test
    void returns401_onBadCredentials() throws Exception {
        LoginRequest req = new LoginRequest();
        req.setEmail("alice@example.com");
        req.setPassword("WrongPass!");
        req.setDeviceId("d1");

        mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.title").value("Authentication Failed"));
    }

    

    @Test
    void returns401_whenNoJwtProvided() throws Exception {
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    

    @Test
    void returns403_whenRefreshTokenNotFound() throws Exception {
        RefreshTokenRequest req = new RefreshTokenRequest();
        req.setRefreshToken("nonexistent");

        mockMvc.perform(post("/auth/refresh")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Token Refresh Failed"));
    }

    

    @Test
    void returns403_whenNotMemberOfConversation() throws Exception {
        User bob   = createUser("bob@example.com", "Bob");
        User carol = createUser("carol@example.com", "Carol");

        createDirectConversation(bob, carol); 

        long convId = conversationRepository.findAll().get(0).getId();

        mockMvc.perform(get("/conversations/{id}/keys", convId)
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.title").value("Access Denied"));
    }

   

    @Test
    void errorResponse_containsAllRequiredFields() throws Exception {
        mockMvc.perform(get("/auth/salt").param("email", "nobody@example.com"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.type").value("about:blank"))
                .andExpect(jsonPath("$.title").isString())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.detail").isString())
                .andExpect(jsonPath("$.instance").isString());
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
