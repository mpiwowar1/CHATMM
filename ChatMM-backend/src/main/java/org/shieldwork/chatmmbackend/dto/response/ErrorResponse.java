package org.shieldwork.chatmmbackend.dto.response;

import lombok.Builder;
import lombok.Data;
//import java.time.LocalDateTime;

// RFC 9457
@Data
@Builder
public class ErrorResponse {
    private String type;
    private String title;
    private int status;
    private String detail;
    private String instance;
//    @Builder.Default
//    private LocalDateTime timestamp = LocalDateTime.now();
}