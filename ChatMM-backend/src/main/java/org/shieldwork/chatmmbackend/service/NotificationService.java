package org.shieldwork.chatmmbackend.service;

import lombok.RequiredArgsConstructor;
import org.shieldwork.chatmmbackend.dto.response.NotificationResponse;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final SimpMessagingTemplate messagingTemplate;
    private final ParticipantRepository participantRepository;

    @Transactional(readOnly = true)
    public void sendNewMessageNotification(Long conversationId, Long senderId, String senderName, String senderEmail) {
        List<Participant> participants = participantRepository.findAllByConversationId(conversationId);

        NotificationResponse notification = NotificationResponse.builder()
                .conversationId(conversationId)
                .senderId(senderId)
                .senderName(senderName)
                .timestamp(LocalDateTime.now())
                .build();

        for (Participant participant : participants) {
            String targetEmail = participant.getUser().getEmail();

            if (!targetEmail.equals(senderEmail)) {
                messagingTemplate.convertAndSendToUser(
                        targetEmail,
                        "/queue/notifications",
                        notification
                );
            }
        }
    }
}