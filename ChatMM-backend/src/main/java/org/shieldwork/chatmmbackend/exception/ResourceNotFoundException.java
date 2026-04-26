package org.shieldwork.chatmmbackend.exception;

// inheritance?
// based of messages?
// Custom enum ErrorCode?
public class ResourceNotFoundException extends RuntimeException {
    public ResourceNotFoundException(String message) {
        super(message);
    }
}