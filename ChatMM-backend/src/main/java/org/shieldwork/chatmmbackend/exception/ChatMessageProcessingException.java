package org.shieldwork.chatmmbackend.exception;

public class ChatMessageProcessingException extends RuntimeException {
    public ChatMessageProcessingException(String message) {
        super(message);
    }
}