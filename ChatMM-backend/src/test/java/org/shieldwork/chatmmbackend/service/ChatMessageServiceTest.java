package org.shieldwork.chatmmbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.ChatMessagePayload;
import org.shieldwork.chatmmbackend.dto.response.ChatMessageResponse;
import org.shieldwork.chatmmbackend.dto.response.MessagePageResponse;
import org.shieldwork.chatmmbackend.exception.ChatMessageProcessingException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Message;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.MessageRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.springframework.data.domain.Pageable;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatMessageServiceTest {

    @Mock private MessageRepository messageRepository;
    @Mock private ConversationRepository conversationRepository;
    @Mock private UserRepository userRepository;
    @Mock private ParticipantRepository participantRepository;

    @InjectMocks
    private ChatMessageService chatMessageService;

    @Test
    void processAndSaveMessage_ShouldSaveAndReturnMessage_WhenUserIsMember() {
        String senderEmail = "sender@example.com";
        Long conversationId = 100L;

        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(conversationId);
        payload.setCiphertext("encrypted_hello");
        payload.setIv("init_vector");

        User sender = User.builder().id(1L).email(senderEmail).name("Sender").build();
        Conversation conversation = Conversation.builder().id(conversationId).build();

        Message savedMessage = Message.builder()
                .id(500L)
                .conversation(conversation)
                .sender(sender)
                .ciphertext("encrypted_hello")
                .iv("init_vector")
                .timestamp(LocalDateTime.now())
                .build();

        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));
        when(participantRepository.existsByConversationIdAndUserEmail(conversationId, senderEmail)).thenReturn(true);
        when(messageRepository.save(any(Message.class))).thenReturn(savedMessage);

        ChatMessageResponse response = chatMessageService.processAndSaveMessage(payload, senderEmail);

        assertNotNull(response, "Response should not be null");
        assertEquals(500L, response.getId(), "Message ID should match saved message");
        assertEquals(conversationId, response.getConversationId(), "Conversation ID should match");
        assertEquals(1L, response.getSenderId(), "Sender ID should match");
        assertEquals("encrypted_hello", response.getCiphertext(), "Ciphertext should match");

        verify(messageRepository, times(1)).save(any(Message.class));
    }

    @Test
    void processAndSaveMessage_ShouldThrowException_WhenUserIsNotMember() {
        String senderEmail = "notMember@example.com";
        Long conversationId = 100L;

        ChatMessagePayload payload = new ChatMessagePayload();
        payload.setConversationId(conversationId);

        User sender = User.builder().id(2L).email(senderEmail).build();
        Conversation conversation = Conversation.builder().id(conversationId).build();

        when(userRepository.findByEmail(senderEmail)).thenReturn(Optional.of(sender));
        when(conversationRepository.findById(conversationId)).thenReturn(Optional.of(conversation));

        when(participantRepository.existsByConversationIdAndUserEmail(conversationId, senderEmail)).thenReturn(false);

        ChatMessageProcessingException exception = assertThrows(
                ChatMessageProcessingException.class,
                () -> chatMessageService.processAndSaveMessage(payload, senderEmail),
                "Should throw exception if user is not a member of the conversation"
        );

        assertEquals("You do not have permission to post in this conversation", exception.getMessage());
        verify(messageRepository, never()).save(any(Message.class));
    }

    @Test
    void getConversationMessages_ShouldReturnMessages_WhenNoCursorProvided() {
        Long conversationId = 100L;
        String userEmail = "user@example.com";
        int limit = 2;

        when(participantRepository.existsByConversationIdAndUserEmail(conversationId, userEmail)).thenReturn(true);

        User sender = User.builder().id(1L).name("User").build();
        Message msg2 = Message.builder().id(2L).sender(sender).ciphertext("msg2").build();
        Message msg1 = Message.builder().id(1L).sender(sender).ciphertext("msg1").build();

        when(messageRepository.findByConversationIdOrderByIdDesc(eq(conversationId), any(Pageable.class)))
                .thenReturn(List.of(msg2, msg1));

        MessagePageResponse response = chatMessageService.getConversationMessages(conversationId, userEmail, null, limit);

        assertNotNull(response);
        assertEquals(2, response.getMessages().size(), "Should return exactly 2 messages");
        assertTrue(response.isHasMore(), "hasMore should be true because we fetched exactly 'limit' amount of messages");

        assertEquals(1L, response.getMessages().get(0).getId(), "First message should be the oldest one (sorted ascending)");
        assertEquals(2L, response.getMessages().get(1).getId(), "Second message should be the newest one");

        assertEquals(1L, response.getNextCursor(), "Next cursor should point to the last fetched message ID");
    }
}