package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;

@Data
@Builder
public class ConversationParticipantResponse {
    private Long id;
    private String email;
    private String name;
    private ParticipantRole role;
}