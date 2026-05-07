package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;

@Data
@Builder
public class ConversationSummaryResponse {
    private Long id;
    private String name;
    private ConversationType type;
    private String encryptedAesKey;

    // message info
}