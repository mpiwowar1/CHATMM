package org.shieldwork.chatmmbackend.advice;

import org.shieldwork.chatmmbackend.exception.ChatMessageProcessingException;
import org.springframework.messaging.handler.annotation.MessageExceptionHandler;
import org.springframework.web.bind.annotation.ControllerAdvice;

@ControllerAdvice
public class WebSocketExceptionHandler {

    @MessageExceptionHandler(ChatMessageProcessingException.class)
    public void handleChatMessageProcessingException(ChatMessageProcessingException ex) {
    }
}
