package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private LocalDateTime timestamp;
}