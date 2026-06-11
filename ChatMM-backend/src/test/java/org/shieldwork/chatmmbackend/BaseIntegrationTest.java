package org.shieldwork.chatmmbackend;

import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.MessageRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.RefreshTokenRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.shieldwork.chatmmbackend.security.JwtService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import com.fasterxml.jackson.databind.ObjectMapper;


@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
public abstract class BaseIntegrationTest {

    @Autowired protected MockMvc mockMvc;
    @Autowired protected ObjectMapper objectMapper;
    @Autowired protected UserRepository userRepository;
    @Autowired protected ConversationRepository conversationRepository;
    @Autowired protected ParticipantRepository participantRepository;
    @Autowired protected MessageRepository messageRepository;
    @Autowired protected RefreshTokenRepository refreshTokenRepository;
    @Autowired protected JwtService jwtService;
    @Autowired protected PasswordEncoder passwordEncoder;

    protected User createUser(String email, String name) {
        User user = User.builder()
                .email(email)
                .name(name)
                .password(passwordEncoder.encode("Password1!"))
                .frontSalt("test-salt")
                .publicKey("public-key-" + email)
                .encryptedPrivateKey("enc-priv-key-" + email)
                .build();
        return userRepository.save(user);
    }

    protected String bearerToken(User user) {
        return "Bearer " + jwtService.generateToken(user);
    }

    protected Conversation createDirectConversation(User a, User b) {
        Conversation conv = new Conversation();
        conv.setType(ConversationType.DIRECT);
        Conversation saved = conversationRepository.save(conv);

        participantRepository.save(Participant.builder()
                .user(a).conversation(saved).encryptedAesKey("aes-a")
                .role(ParticipantRole.ADMIN).build());
        participantRepository.save(Participant.builder()
                .user(b).conversation(saved).encryptedAesKey("aes-b")
                .role(ParticipantRole.ADMIN).build());

        return saved;
    }
}
