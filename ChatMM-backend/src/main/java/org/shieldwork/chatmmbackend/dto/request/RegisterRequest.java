package org.shieldwork.chatmmbackend.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

// MethodArgumentNotValidException handling
@Data
public class RegisterRequest {

    @NotBlank(message = "Email address is required")
    @Email(message = "Please provide a valid email address")
    private String email;

    @NotBlank(message = "Name is required")
    @Size(min = 2, max = 50, message = "Name must be between 2 and 50 characters")
    private String name;

    // regex? size?
    @NotBlank(message = "Password is required")
    private String password;

    @NotBlank(message = "Frontend salt is required")
    private String frontSalt;

    @NotBlank(message = "RSA public key is required")
    private String publicKey;

    @NotBlank(message = "Encrypted private key is required")
    private String encryptedPrivateKey;
}