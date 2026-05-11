package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class ConversationSummaryResponse {
    private Long id;
    private String name;
    private ConversationType type;
    private String encryptedAesKey;
    private LocalDateTime lastMessageAt;
    private String lastMessagePreview;
    private String lastMessageIv;
    private String lastMessageSenderName;
    private List<ConversationParticipantResponse> participants;
}