package org.shieldwork.chatmmbackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ConversationCreatedResponse {
    private Long conversationId;
}