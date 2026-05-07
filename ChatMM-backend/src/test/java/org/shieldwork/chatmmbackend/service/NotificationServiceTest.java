package org.shieldwork.chatmmbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.response.NotificationResponse;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private SimpMessagingTemplate messagingTemplate;
    @Mock private ParticipantRepository participantRepository;

    @InjectMocks
    private NotificationService notificationService;

    @Test
    void sendNewMessageNotification_ShouldSendToAllParticipants_ExceptSender() {
        Long conversationId = 1L;
        Long senderId = 100L;
        String senderName = "Alice";
        String senderEmail = "alice@example.com";

        User sender = User.builder().id(senderId).email(senderEmail).build();
        User receiver1 = User.builder().id(101L).email("bob@example.com").build();
        User receiver2 = User.builder().id(102L).email("charlie@example.com").build();

        Participant p1 = Participant.builder().user(sender).build();
        Participant p2 = Participant.builder().user(receiver1).build();
        Participant p3 = Participant.builder().user(receiver2).build();

        when(participantRepository.findAllByConversationId(conversationId))
                .thenReturn(List.of(p1, p2, p3));

        notificationService.sendNewMessageNotification(conversationId, senderId, senderName, senderEmail);

        ArgumentCaptor<NotificationResponse> notificationCaptor = ArgumentCaptor.forClass(NotificationResponse.class);

        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("bob@example.com"), eq("/queue/notifications"), notificationCaptor.capture()
        );
        verify(messagingTemplate, times(1)).convertAndSendToUser(
                eq("charlie@example.com"), eq("/queue/notifications"), notificationCaptor.capture()
        );

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq(senderEmail), anyString(), any()
        );

        NotificationResponse capturedNotification = notificationCaptor.getValue();
        assertNotNull(capturedNotification);
        assertEquals(conversationId, capturedNotification.getConversationId(), "Conversation ID should match");
        assertEquals(senderId, capturedNotification.getSenderId(), "Sender ID should match");
        assertEquals(senderName, capturedNotification.getSenderName(), "Sender name should match");
        assertNotNull(capturedNotification.getTimestamp(), "Timestamp should be generated");
    }

    @Test
    void sendNewMessageNotification_ShouldNotSendAnything_WhenSenderIsTheOnlyParticipant() {
        Long conversationId = 2L;
        String senderEmail = "lonely@example.com";

        User sender = User.builder().id(99L).email(senderEmail).build();
        Participant alone = Participant.builder().user(sender).build();

        when(participantRepository.findAllByConversationId(conversationId))
                .thenReturn(List.of(alone));

        notificationService.sendNewMessageNotification(conversationId, 99L, "Lonely User", senderEmail);

        verify(messagingTemplate, never()).convertAndSendToUser(anyString(), anyString(), any());
    }
}