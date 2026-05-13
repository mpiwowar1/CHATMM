package org.shieldwork.chatmmbackend.advice;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.MalformedJwtException;
import io.jsonwebtoken.security.SignatureException;
import jakarta.servlet.http.HttpServletRequest;
import org.shieldwork.chatmmbackend.dto.response.ErrorResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.exception.TokenRefreshException;
import org.shieldwork.chatmmbackend.exception.UserAlreadyExistsException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import java.util.stream.Collectors;

@RestControllerAdvice
public class RestExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFoundException(ResourceNotFoundException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.NOT_FOUND, "Resource Not Found", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ErrorResponse> handleUserAlreadyExistsException(UserAlreadyExistsException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.CONFLICT, "User Already Exists", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(TokenRefreshException.class)
    public ResponseEntity<ErrorResponse> handleTokenRefreshException(TokenRefreshException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.FORBIDDEN, "Token Refresh Failed", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<ErrorResponse> handleIllegalArgumentException(IllegalArgumentException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.BAD_REQUEST, "Bad Request", ex.getMessage(), request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ErrorResponse> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        String detail = ex.getBindingResult().getFieldErrors().stream()
                .map(FieldError::getDefaultMessage)
                .collect(Collectors.joining(", "));

        return buildResponse("about:blank", HttpStatus.UNPROCESSABLE_CONTENT, "Validation Failed", detail, request.getRequestURI());
    }

    @ExceptionHandler(BadCredentialsException.class)
    public ResponseEntity<ErrorResponse> handleBadCredentialsException(BadCredentialsException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.UNAUTHORIZED, "Authentication Failed", "Invalid email or password.", request.getRequestURI());
    }

    @ExceptionHandler({ExpiredJwtException.class, SignatureException.class, MalformedJwtException.class})
    public ResponseEntity<ErrorResponse> handleJwtExceptions(Exception ex, HttpServletRequest request) {
        String detail = ex instanceof ExpiredJwtException ? "JWT token has expired." : "Invalid JWT token.";
        return buildResponse("about:blank", HttpStatus.UNAUTHORIZED, "Unauthorized", detail, request.getRequestURI());
    }

    @ExceptionHandler(AccessDeniedException.class)
    public ResponseEntity<ErrorResponse> handleAccessDeniedException(AccessDeniedException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.FORBIDDEN, "Access Denied", "You do not have permission to access this resource.", request.getRequestURI());
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGlobalException(Exception ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.INTERNAL_SERVER_ERROR, "Internal Server Error", "An unexpected error occurred.", request.getRequestURI());
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<ErrorResponse> handleHttpMessageNotReadableException(HttpMessageNotReadableException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.BAD_REQUEST, "Malformed JSON Request", "The request body is missing or could not be parsed. Please check your JSON syntax.", request.getRequestURI());
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<ErrorResponse> handleMethodNotSupportedException(HttpRequestMethodNotSupportedException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.METHOD_NOT_ALLOWED, "Method Not Allowed", "The HTTP method '" + ex.getMethod() + "' is not supported for this endpoint.", request.getRequestURI());
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<ErrorResponse> handleMissingParams(MissingServletRequestParameterException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.BAD_REQUEST, "Missing Request Parameter", "The required parameter '" + ex.getParameterName() + "' is missing.", request.getRequestURI());
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(DataIntegrityViolationException ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.CONFLICT, "Data Conflict", "The action could not be completed due to a conflict with existing data (e.g., duplicate record).", request.getRequestURI());
    }

    @ExceptionHandler(org.springframework.security.core.AuthenticationException.class)
    public ResponseEntity<ErrorResponse> handleAuthenticationException(Exception ex, HttpServletRequest request) {
        return buildResponse("about:blank", HttpStatus.UNAUTHORIZED, "Unauthorized", "Full authentication is required to access this resource.", request.getRequestURI());
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<ErrorResponse> handleTypeMismatchException(MethodArgumentTypeMismatchException ex, HttpServletRequest request) {
        String requiredTypeName = ex.getRequiredType() != null ? ex.getRequiredType().getSimpleName() : "unknown";
        String detail = String.format("Invalid value provided for parameter '%s'. Expected type '%s'.", ex.getName(), requiredTypeName);

        return buildResponse("about:blank", HttpStatus.BAD_REQUEST, "Invalid Parameter Type", detail, request.getRequestURI());
    }

    private ResponseEntity<ErrorResponse> buildResponse(String type, HttpStatus status, String title, String detail, String instance) {
        ErrorResponse errorResponse = ErrorResponse.builder()
                .type(type)
                .title(title)
                .status(status.value())
                .detail(detail)
                .instance(instance)
                .build();

        return ResponseEntity.status(status).body(errorResponse);
    }
}