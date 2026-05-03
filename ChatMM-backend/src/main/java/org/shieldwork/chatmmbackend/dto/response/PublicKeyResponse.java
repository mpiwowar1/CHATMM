package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class PublicKeyResponse {
    private Long userId;
    private String publicKey;
}