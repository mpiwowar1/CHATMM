package org.shieldwork.chatmmbackend.advice;

import io.jsonwebtoken.ExpiredJwtException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@RestControllerAdvice
public class RestExceptionHandler {
    @ExceptionHandler(RuntimeException.class)
    public void handleRestException(RuntimeException ex) {
    }

    @ExceptionHandler(ExpiredJwtException.class)
    public void handleExpiredJwtException(ExpiredJwtException ex) {
    }
}