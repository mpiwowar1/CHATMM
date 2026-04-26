package org.shieldwork.chatmmbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;
import java.util.Map;

@Data
public class CreateConversationRequest {
    private String name;

    @NotEmpty(message = "Conversation must have at least one invited participant")
    private Map<Long, @NotBlank(message = "Participant key cannot be blank") String> participantKeys;
}