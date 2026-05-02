package org.shieldwork.chatmmbackend.controller;

import org.shieldwork.chatmmbackend.dto.request.ChatMessagePayload;
import org.shieldwork.chatmmbackend.dto.response.ChatMessageResponse;
import org.shieldwork.chatmmbackend.service.ChatMessageService;
import lombok.RequiredArgsConstructor;
import org.shieldwork.chatmmbackend.service.NotificationService;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;

@Controller
@RequiredArgsConstructor
public class ChatController {

    private final SimpMessagingTemplate messagingTemplate;
    private final ChatMessageService chatMessageService;
    private final NotificationService notificationService;

    @MessageMapping("/chat.send")
    public void sendMessage(@Payload ChatMessagePayload payload, Authentication authentication) {
        String senderEmail = authentication.getName();

        ChatMessageResponse response = chatMessageService.processAndSaveMessage(payload, senderEmail);

        String destination = "/topic/conversation." + payload.getConversationId();
        messagingTemplate.convertAndSend(destination, response);

        notificationService.sendNewMessageNotification(
                payload.getConversationId(),
                response.getSenderId(),
                response.getSenderName(),
                senderEmail
        );
    }
}
