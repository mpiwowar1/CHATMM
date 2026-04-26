package org.shieldwork.chatmmbackend.dto.request;

import lombok.Data;

@Data
public class RefreshTokenRequest {
    private String refreshToken;
}