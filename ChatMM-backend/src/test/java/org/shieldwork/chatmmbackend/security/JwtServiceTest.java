package org.shieldwork.chatmmbackend.security;

import io.jsonwebtoken.ExpiredJwtException;
import io.jsonwebtoken.security.SignatureException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.*;

class JwtServiceTest {

    private JwtService jwtService;
    private User testUser;

    private final String SECRET_KEY = "e4f38b15586aa0262379d459bd7409a9b48fe61bae514357103efeb5b8f0d962==";

    private final long EXPIRATION_TIME = 1000 * 60 * 60;

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();

        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET_KEY);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", EXPIRATION_TIME);

        testUser = User.builder()
                .id(1L)
                .email("test@example.com")
                .password("encodedPassword")
                .name("Test User")
                .build();
    }

    @Test
    void generateToken_ShouldReturnToken_WhenUserDetailsAreProvided() {
        String token = jwtService.generateToken(testUser);

        assertNotNull(token, "Generated token should not be null");
        assertFalse(token.isEmpty(), "Generated token should not be empty");
    }

    @Test
    void extractUsername_ShouldReturnEmail_WhenTokenIsValid() {
        String token = jwtService.generateToken(testUser);

        String extractedUsername = jwtService.extractUsername(token);

        assertEquals("test@example.com", extractedUsername, "Extracted username should exactly match the user's email");
    }

    @Test
    void isTokenValid_ShouldReturnTrue_WhenTokenMatchesUser() {
        String token = jwtService.generateToken(testUser);

        boolean isValid = jwtService.isTokenValid(token, testUser);

        assertTrue(isValid, "Token should be valid for the user it was generated for");
    }

    @Test
    void isTokenValid_ShouldReturnFalse_WhenTokenBelongsToDifferentUser() {
        String token = jwtService.generateToken(testUser);

        User differentUser = User.builder()
                .email("other@example.com")
                .build();

        boolean isValid = jwtService.isTokenValid(token, differentUser);

        assertFalse(isValid, "Token should not be valid when verified against a different user");
    }

    @Test
    void extractUsername_ShouldThrowException_WhenTokenIsExpired() throws InterruptedException {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 1L);
        String token = jwtService.generateToken(testUser);

        Thread.sleep(10);

        ExpiredJwtException exception = assertThrows(
                ExpiredJwtException.class,
                () -> jwtService.extractUsername(token),
                "Should throw ExpiredJwtException when parsing an expired token"
        );

        assertNotNull(exception, "Exception should be thrown");
    }

    @Test
    void extractUsername_ShouldThrowException_WhenSignatureIsInvalid() {
        String token = jwtService.generateToken(testUser);

        ReflectionTestUtils.setField(jwtService, "secretKey", "72d57e5b751c8ecda09a63c90bf81954a81170d0ecc61bae02b5ec55baa35fc5");

        SignatureException exception = assertThrows(
                SignatureException.class,
                () -> jwtService.extractUsername(token),
                "Should throw SignatureException when token is signed with a different key"
        );

        assertNotNull(exception, "Exception should be thrown");
    }
}