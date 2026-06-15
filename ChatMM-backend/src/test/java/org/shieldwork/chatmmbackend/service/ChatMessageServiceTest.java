package org.shieldwork.chatmmbackend.service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.ChatMessagePayload;
import org.shieldwork.chatmmbackend.dto.response.ChatMessageResponse;
import org.shieldwork.chatmmbackend.dto.response.MessagePageResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Message;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.MessageRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock MessageRepository messageRepository;
    @Mock ConversationRepository conversationRepository;
    @Mock UserRepository userRepository;
    @Mock ParticipantRepository participantRepository;

    @InjectMocks ChatMessageService chatMessageService;


    @Test
    void processAndSaveMessage_savesAndReturnsResponse() {
        User sender = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);

        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(10L);
        payload.setCiphertext("encrypted-text");
        payload.setIv("iv-value");

        Message saved = Message.builder()
                .id(100L)
                .conversation(conv)
                .sender(sender)
                .ciphertext("encrypted-text")
                .iv("iv-value")
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(saved);
        when(conversationRepository.save(any())).thenReturn(conv);

        ChatMessageResponse response = chatMessageService.processAndSaveMessage(payload, "alice@example.com");

        assertThat(response.getId()).isEqualTo(100L);
        assertThat(response.getConversationId()).isEqualTo(10L);
        assertThat(response.getSenderId()).isEqualTo(1L);
        assertThat(response.getCiphertext()).isEqualTo("encrypted-text");
        assertThat(response.getIv()).isEqualTo("iv-value");

        verify(conversationRepository).save(argThat(c ->
                "encrypted-text".equals(c.getLastMessagePreview()) &&
                "iv-value".equals(c.getLastMessageIv()) &&
                "Alice".equals(c.getLastMessageSenderName())
        ));
    }

    @Test
    void processAndSaveMessage_throws_whenUserNotFound() {
        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(10L);
        payload.setCiphertext("x");
        payload.setIv("y");

        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.processAndSaveMessage(payload, "ghost@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void processAndSaveMessage_throws_whenConversationNotFound() {
        User sender = buildUser(1L, "alice@example.com");
        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(999L);
        payload.setCiphertext("x");
        payload.setIv("y");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(999L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> chatMessageService.processAndSaveMessage(payload, "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void processAndSaveMessage_throws_whenNotMember() {
        User sender = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);
        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(10L);
        payload.setCiphertext("x");
        payload.setIv("y");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(10L)).thenReturn(Optional.of(conv));
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.processAndSaveMessage(payload, "alice@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    void getConversationMessages_returnsPage_withoutCursor() {
        User sender = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);

        Message msg1 = buildMessage(1L, conv, sender);
        Message msg2 = buildMessage(2L, conv, sender);

        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(true);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(10L), any()))
                .thenReturn(List.of(msg2, msg1));

        MessagePageResponse response = chatMessageService.getConversationMessages(10L, "alice@example.com", null, 50);

        assertThat(response.getMessages()).hasSize(2);
        assertThat(response.getMessages().get(0).getId()).isEqualTo(1L);
        assertThat(response.getMessages().get(1).getId()).isEqualTo(2L);
        assertThat(response.getNextCursor()).isEqualTo(1L); 
        assertThat(response.isHasMore()).isFalse(); 
    }

    @Test
    void getConversationMessages_returnsPage_withCursor() {
        User sender = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);
        Message msg = buildMessage(3L, conv, sender);

        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(true);
        when(messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(eq(10L), eq(5L), any()))
                .thenReturn(List.of(msg));

        MessagePageResponse response = chatMessageService.getConversationMessages(10L, "alice@example.com", 5L, 50);

        assertThat(response.getMessages()).hasSize(1);
        assertThat(response.getNextCursor()).isEqualTo(3L);
    }

    @Test
    void getConversationMessages_returnsEmpty_whenNoMessages() {
        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(true);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(10L), any())).thenReturn(List.of());

        MessagePageResponse response = chatMessageService.getConversationMessages(10L, "alice@example.com", null, 50);

        assertThat(response.getMessages()).isEmpty();
        assertThat(response.getNextCursor()).isNull();
        assertThat(response.isHasMore()).isFalse();
    }

    @Test
    void getConversationMessages_setsHasMore_whenFullPage() {
        User sender = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);

        List<Message> messages = List.of(
                buildMessage(1L, conv, sender),
                buildMessage(2L, conv, sender)
        );

        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(true);
        when(messageRepository.findByConversationIdOrderByIdDesc(eq(10L), any())).thenReturn(messages);

        MessagePageResponse response = chatMessageService.getConversationMessages(10L, "alice@example.com", null, 2);

        assertThat(response.isHasMore()).isTrue();
    }

    @Test
    void getConversationMessages_throws_whenConversationNotFound() {
        when(conversationRepository.existsById(999L)).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.getConversationMessages(999L, "alice@example.com", null, 50))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getConversationMessages_throws_whenNotMember() {
        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.existsByConversationIdAndUserEmail(10L, "alice@example.com")).thenReturn(false);

        assertThatThrownBy(() -> chatMessageService.getConversationMessages(10L, "alice@example.com", null, 50))
                .isInstanceOf(AccessDeniedException.class);
    }


    private User buildUser(Long id, String email) {
        return User.builder().id(id).email(email).name("Alice")
                .password("p").frontSalt("s").publicKey("k").encryptedPrivateKey("e").build();
    }

    private Conversation buildConversation(Long id) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setType(ConversationType.DIRECT);
        return c;
    }

    private Message buildMessage(Long id, Conversation conv, User sender) {
        return Message.builder()
                .id(id)
                .conversation(conv)
                .sender(sender)
                .ciphertext("ct")
                .iv("iv")
                .timestamp(LocalDateTime.now())
                .build();
    }
}
