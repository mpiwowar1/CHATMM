package org.shieldwork.chatmmbackend.security;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.test.util.ReflectionTestUtils;

class JwtServiceTest {

    private JwtService jwtService;

    
    private static final String SECRET = "eed9739e4b81a345558172e4b526b7589dd2db76a92ad4a8fd215cd89552432a";

    @BeforeEach
    void setUp() {
        jwtService = new JwtService();
        ReflectionTestUtils.setField(jwtService, "secretKey", SECRET);
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", 900_000L);
    }

    private User makeUser(String email) {
        return User.builder()
                .id(1L)
                .email(email)
                .name("Test User")
                .password("hashed")
                .frontSalt("salt")
                .publicKey("pubkey")
                .encryptedPrivateKey("encpriv")
                .build();
    }

    @Test
    void generateToken_returnsNonBlankToken() {
        User user = makeUser("test@example.com");
        String token = jwtService.generateToken(user);
        assertThat(token).isNotBlank();
    }

    @Test
    void extractUsername_returnsCorrectEmail() {
        User user = makeUser("alice@example.com");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.extractUsername(token)).isEqualTo("alice@example.com");
    }

    @Test
    void isTokenValid_returnsTrueForCorrectUser() {
        User user = makeUser("bob@example.com");
        String token = jwtService.generateToken(user);
        assertThat(jwtService.isTokenValid(token, user)).isTrue();
    }

    @Test
    void isTokenValid_returnsFalseForDifferentUser() {
        User alice = makeUser("alice@example.com");
        User bob = makeUser("bob@example.com");
        String token = jwtService.generateToken(alice);
        assertThat(jwtService.isTokenValid(token, bob)).isFalse();
    }

    @Test
    void expiredToken_throwsException() {
        ReflectionTestUtils.setField(jwtService, "jwtExpiration", -1L);
        User user = makeUser("expired@example.com");
        String token = jwtService.generateToken(user);

        
        assertThatException().isThrownBy(() -> jwtService.extractUsername(token));
    }
}
