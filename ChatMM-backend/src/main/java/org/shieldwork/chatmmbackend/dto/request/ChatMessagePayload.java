package org.shieldwork.chatmmbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class ChatMessagePayload {
    @NotNull(message = "Conversation ID is required")
    private Long conversationId;

    @NotBlank(message = "Ciphertext is required")
    private String ciphertext;

    @NotBlank(message = "Initialization vector (IV) is required")
    private String iv;
}
