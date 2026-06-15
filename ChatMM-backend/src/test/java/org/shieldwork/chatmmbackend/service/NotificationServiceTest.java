package org.shieldwork.chatmmbackend.service;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.response.ConversationSummaryResponse;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @Mock ParticipantRepository participantRepository;

    @InjectMocks NotificationService notificationService;


    @Test
    void sendNewMessageNotification_sendsToAllOtherParticipants() {
        User alice = buildUser(1L, "alice@example.com");
        User bob   = buildUser(2L, "bob@example.com");
        User carol = buildUser(3L, "carol@example.com");

        Conversation conv = buildConversation(10L);

        when(participantRepository.findAllByConversationId(10L)).thenReturn(List.of(
                buildParticipant(alice, conv),
                buildParticipant(bob, conv),
                buildParticipant(carol, conv)
        ));

        notificationService.sendNewMessageNotification(10L, 1L, "Alice", "alice@example.com");

        verify(messagingTemplate, never()).convertAndSendToUser(
                eq("alice@example.com"), anyString(), any());
        verify(messagingTemplate).convertAndSendToUser(
                eq("bob@example.com"), eq("/queue/notifications"), any());
        verify(messagingTemplate).convertAndSendToUser(
                eq("carol@example.com"), eq("/queue/notifications"), any());
    }

    @Test
    void sendNewMessageNotification_sendsToNobody_whenSenderIsOnlyParticipant() {
        User alice = buildUser(1L, "alice@example.com");
        Conversation conv = buildConversation(10L);

        when(participantRepository.findAllByConversationId(10L))
                .thenReturn(List.of(buildParticipant(alice, conv)));

        notificationService.sendNewMessageNotification(10L, 1L, "Alice", "alice@example.com");

        verifyNoInteractions(messagingTemplate);
    }

    @Test
    void sendNewMessageNotification_notificationContainsCorrectConversationId() {
        User alice = buildUser(1L, "alice@example.com");
        User bob   = buildUser(2L, "bob@example.com");
        Conversation conv = buildConversation(10L);

        when(participantRepository.findAllByConversationId(10L))
                .thenReturn(List.of(buildParticipant(alice, conv), buildParticipant(bob, conv)));

        notificationService.sendNewMessageNotification(10L, 1L, "Alice", "alice@example.com");

        verify(messagingTemplate).convertAndSendToUser(
                eq("bob@example.com"),
                eq("/queue/notifications"),
                argThat(payload -> {
                    var notification = (org.shieldwork.chatmmbackend.dto.response.NotificationResponse) payload;
                    return notification.getConversationId().equals(10L)
                            && notification.getSenderId().equals(1L)
                            && "Alice".equals(notification.getSenderName());
                })
        );
    }


    @Test
    void sendNewConversationNotification_sendsToCorrectDestination() {
        ConversationSummaryResponse summary = ConversationSummaryResponse.builder()
                .id(10L)
                .name("Test Conv")
                .type(ConversationType.DIRECT)
                .build();

        notificationService.sendNewConversationNotification("bob@example.com", summary);

        verify(messagingTemplate).convertAndSendToUser(
                eq("bob@example.com"),
                eq("/queue/conversations"),
                eq(summary)
        );
    }

    @Test
    void sendNewConversationNotification_doesNotCallParticipantRepository() {
        notificationService.sendNewConversationNotification("bob@example.com",
                ConversationSummaryResponse.builder().id(1L).build());

        verifyNoInteractions(participantRepository);
    }


    private User buildUser(Long id, String email) {
        return User.builder().id(id).email(email).name(email.split("@")[0])
                .password("p").frontSalt("s").publicKey("k").encryptedPrivateKey("e").build();
    }

    private Conversation buildConversation(Long id) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setType(ConversationType.DIRECT);
        return c;
    }

    private Participant buildParticipant(User user, Conversation conv) {
        return Participant.builder()
                .user(user).conversation(conv)
                .encryptedAesKey("aes").role(ParticipantRole.ADMIN).build();
    }
}
