package org.shieldwork.chatmmbackend.controller;

import java.time.LocalDateTime;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.BaseIntegrationTest;
import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Message;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.http.MediaType;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import org.springframework.transaction.annotation.Transactional;

@Transactional
class ConversationControllerIT extends BaseIntegrationTest {

    private User alice;
    private User bob;

    @BeforeEach
    void setup() {
        messageRepository.deleteAll();
        participantRepository.deleteAll();
        conversationRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();

        alice = createUser("alice@example.com", "Alice");
        bob   = createUser("bob@example.com", "Bob");
    }

   

    @Test
    void createConversation_returns201_forDirectChat() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(alice.getId(), "key-a", bob.getId(), "key-b"));

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").isNumber());

        assertThat(conversationRepository.count()).isEqualTo(1);
        assertThat(participantRepository.count()).isEqualTo(2);
    }

    @Test
    void createConversation_returns201_forGroupChat() throws Exception {
        User carol = createUser("carol@example.com", "Carol");

        CreateConversationRequest req = new CreateConversationRequest();
        req.setName("Group");
        req.setParticipantKeys(Map.of(
                alice.getId(), "key-a",
                bob.getId(), "key-b",
                carol.getId(), "key-c"
        ));

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isCreated());

        assertThat(participantRepository.count()).isEqualTo(3);
    }

    @Test
    void createConversation_returns409_whenDirectAlreadyExists() throws Exception {
        createDirectConversation(alice, bob);

        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(alice.getId(), "key-a", bob.getId(), "key-b"));

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isConflict());
    }

    @Test
    void createConversation_returns400_whenCreatorNotInParticipants() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(bob.getId(), "key-b", 999L, "key-x")); 

        mockMvc.perform(post("/conversations")
                        .header("Authorization", bearerToken(alice))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest());
    }

    @Test
    void createConversation_returns401_withoutAuth() throws Exception {
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(alice.getId(), "k1", bob.getId(), "k2"));

        mockMvc.perform(post("/conversations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isUnauthorized());
    }

    

    @Test
    void getParticipantPublicKeys_returns200_withOtherParticipantsKeys() throws Exception {
        Conversation conv = createDirectConversation(alice, bob);

        mockMvc.perform(get("/conversations/{id}/keys", conv.getId())
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray())
                .andExpect(jsonPath("$[0].userId").value(bob.getId()))
                .andExpect(jsonPath("$[0].publicKey").value("public-key-bob@example.com"));
    }

    @Test
    void getParticipantPublicKeys_returns403_whenNotMember() throws Exception {
        User carol = createUser("carol@example.com", "Carol");
        Conversation conv = createDirectConversation(alice, bob);

        mockMvc.perform(get("/conversations/{id}/keys", conv.getId())
                        .header("Authorization", bearerToken(carol)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getParticipantPublicKeys_returns404_whenConversationNotFound() throws Exception {
        mockMvc.perform(get("/conversations/9999/keys")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isNotFound());
    }


    @Test
    void getMessages_returns200_withEmptyList_whenNoMessages() throws Exception {
        Conversation conv = createDirectConversation(alice, bob);

        mockMvc.perform(get("/conversations/{id}/messages", conv.getId())
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages").isArray())
                .andExpect(jsonPath("$.messages").isEmpty())
                .andExpect(jsonPath("$.hasMore").value(false));
    }

    @Test
    void getMessages_returns200_withMessages() throws Exception {
        Conversation conv = createDirectConversation(alice, bob);
        saveMessage(conv, alice, "cipher1", "iv1");
        saveMessage(conv, alice, "cipher2", "iv2");

        mockMvc.perform(get("/conversations/{id}/messages", conv.getId())
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }

    @Test
    void getMessages_returns403_whenNotMember() throws Exception {
        User carol = createUser("carol@example.com", "Carol");
        Conversation conv = createDirectConversation(alice, bob);

        mockMvc.perform(get("/conversations/{id}/messages", conv.getId())
                        .header("Authorization", bearerToken(carol)))
                .andExpect(status().isForbidden());
    }

    @Test
    void getMessages_returns404_whenConversationNotFound() throws Exception {
        mockMvc.perform(get("/conversations/9999/messages")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isNotFound());
    }

    @Test
    void getMessages_paginatesCorrectly_withCursor() throws Exception {
        Conversation conv = createDirectConversation(alice, bob);
        Message m1 = saveMessage(conv, alice, "cipher1", "iv1");
        Message m2 = saveMessage(conv, alice, "cipher2", "iv2");
        Message m3 = saveMessage(conv, alice, "cipher3", "iv3");

        mockMvc.perform(get("/conversations/{id}/messages", conv.getId())
                        .param("cursor", String.valueOf(m3.getId()))
                        .param("limit", "10")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.messages.length()").value(2));
    }


    @Test
    void getUserConversations_returns200_withConversationList() throws Exception {
        createDirectConversation(alice, bob);

        mockMvc.perform(get("/conversations")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray())
                .andExpect(jsonPath("$.content.length()").value(1));
    }

    @Test
    void getUserConversations_returns200_withEmptyList_whenNone() throws Exception {
        mockMvc.perform(get("/conversations")
                        .header("Authorization", bearerToken(alice)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isEmpty());
    }

    @Test
    void getUserConversations_returns401_withoutAuth() throws Exception {
        mockMvc.perform(get("/conversations"))
                .andExpect(status().isUnauthorized());
    }


    private Message saveMessage(Conversation conv, User sender, String ciphertext, String iv) {
        Message msg = new Message();
        msg.setConversation(conv);
        msg.setSender(sender);
        msg.setCiphertext(ciphertext);
        msg.setIv(iv);
        msg.setTimestamp(LocalDateTime.now());
        return messageRepository.save(msg);
    }
}
