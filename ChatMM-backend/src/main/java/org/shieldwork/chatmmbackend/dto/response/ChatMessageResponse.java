package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.time.LocalDateTime;

@Data
@Builder
public class ChatMessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private String senderName;
    private String ciphertext;
    private String iv;
    private LocalDateTime timestamp;
}
