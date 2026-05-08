package org.shieldwork.chatmmbackend.websocket;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.shieldwork.chatmmbackend.dto.response.ErrorResponse;
import org.shieldwork.chatmmbackend.exception.ChatMessageProcessingException;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageDeliveryException;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.stereotype.Component;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.socket.messaging.StompSubProtocolErrorHandler;

import java.nio.charset.StandardCharsets;

@Component
@RequiredArgsConstructor
public class StompErrorHandler extends StompSubProtocolErrorHandler {

    private final ObjectMapper objectMapper;

    @Override
    public Message<byte[]> handleClientMessageProcessingError(Message<byte[]> clientMessage, @NonNull Throwable ex) {

        Throwable cause = extractRootCause(ex);

        int status = determineStatusCode(cause);

        String detail = cause.getMessage() != null ? cause.getMessage() : "An unexpected server error occurred.";

        ErrorResponse errorBody = ErrorResponse.builder()
                .type("about:blank")
                .title("WebSocket Error")
                .status(status)
                .detail(detail)
                .instance("stomp-broker")
                .build();

        return prepareErrorMessage(errorBody);
    }

    private int determineStatusCode(Throwable cause) {
        if (cause instanceof SecurityException) {
            return 403;
        } else if (cause instanceof IllegalArgumentException) {
            return 400;
        } else if (cause instanceof ResourceNotFoundException) {
            return 404;
        } else if (cause instanceof ChatMessageProcessingException) {
            return 422;
        }

        return 500;
    }

    private Throwable extractRootCause(Throwable ex) {
        if (ex instanceof MessageDeliveryException && ex.getCause() != null) {
            return ex.getCause();
        }
        return ex;
    }

    private Message<byte[]> prepareErrorMessage(ErrorResponse errorBody) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.ERROR);
        accessor.setMessage(errorBody.getDetail());
        accessor.setContentType(MimeTypeUtils.APPLICATION_JSON);
        accessor.setLeaveMutable(true);

        byte[] payload;
        try {
            payload = objectMapper.writeValueAsBytes(errorBody);
        } catch (JsonProcessingException e) {
            payload = errorBody.getDetail().getBytes(StandardCharsets.UTF_8);
        }

        return MessageBuilder.createMessage(payload, accessor.getMessageHeaders());
    }
}