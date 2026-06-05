package org.shieldwork.chatmmbackend.exception;

public class ConversationAlreadyExistsException extends RuntimeException {
    public ConversationAlreadyExistsException(String message) {
        super(message);
    }
}