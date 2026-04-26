package org.shieldwork.chatmmbackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LogoutRequest {
    @NotBlank(message = "Device ID is required to logout")
    private String deviceId;
}