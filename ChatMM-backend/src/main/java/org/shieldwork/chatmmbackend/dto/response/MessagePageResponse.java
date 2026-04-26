package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;

@Data
@Builder
public class MessagePageResponse {
    private List<ChatMessageResponse> messages;
    private Long nextCursor;
    private boolean hasMore;
}