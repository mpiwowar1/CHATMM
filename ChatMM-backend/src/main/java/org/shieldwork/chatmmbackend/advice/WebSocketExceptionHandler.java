package org.shieldwork.chatmmbackend.advice;

import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.springframework.http.HttpStatus;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.shieldwork.chatmmbackend.dto.response.ErrorResponse;
import org.springframework.messaging.simp.annotation.SendToUser;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(ResourceNotFoundException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleResourceNotFoundException(ResourceNotFoundException ex) {
        return buildErrorResponse("about:blank", HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), "stomp-message-handler");
    }

    @MessageExceptionHandler(IllegalArgumentException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleIllegalArgumentException(IllegalArgumentException ex) {
        return buildErrorResponse("about:blank", HttpStatus.BAD_REQUEST, "Invalid Payload", ex.getMessage(), "stomp-message-handler");
    }

    @MessageExceptionHandler(Exception.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleGlobalException(Exception ex) {
        return buildErrorResponse("about:blank", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred while processing the WebSocket message.", "stomp-message-handler");
    }

    @MessageExceptionHandler(AccessDeniedException.class)
    @SendToUser("/queue/errors")
    public ErrorResponse handleAccessDeniedException(AccessDeniedException ex) {
        return buildErrorResponse("about:blank", HttpStatus.FORBIDDEN, "Access Denied", ex.getMessage(), "stomp-message-handler");
    }

    private ErrorResponse buildErrorResponse(String type, HttpStatus status, String title, String detail, String instance) {
        return ErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(instance)
                .build();
    }
}
