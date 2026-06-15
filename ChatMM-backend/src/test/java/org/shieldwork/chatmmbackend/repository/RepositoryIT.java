package org.shieldwork.chatmmbackend.repository;

import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Message;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.RefreshToken;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.ActiveProfiles;

@DataJpaTest
@ActiveProfiles("test")
class RepositoryIT {

    @Autowired UserRepository userRepository;
    @Autowired ConversationRepository conversationRepository;
    @Autowired ParticipantRepository participantRepository;
    @Autowired MessageRepository messageRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;

    private User alice;
    private User bob;
    private User carol;

    @BeforeEach
    void setup() {
        messageRepository.deleteAll();
        participantRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        conversationRepository.deleteAll();
        userRepository.deleteAll();

        alice = saveUser("alice@example.com", "Alice");
        bob   = saveUser("bob@example.com",   "Bob");
        carol = saveUser("carol@example.com",  "Carol");
    }


    @Test
    void findByEmail_returnsUser_whenExists() {
        Optional<User> result = userRepository.findByEmail("alice@example.com");
        assertThat(result).isPresent();
        assertThat(result.get().getName()).isEqualTo("Alice");
    }

    @Test
    void findByEmail_returnsEmpty_whenNotExists() {
        assertThat(userRepository.findByEmail("nobody@example.com")).isEmpty();
    }

    @Test
    void existsByEmail_returnsTrue_whenExists() {
        assertThat(userRepository.existsByEmail("alice@example.com")).isTrue();
    }

    @Test
    void existsByEmail_returnsFalse_whenNotExists() {
        assertThat(userRepository.existsByEmail("nobody@example.com")).isFalse();
    }

    @Test
    void findByEmailContainingIgnoreCase_returnsMatches() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> results = userRepository.findByEmailContainingIgnoreCase("ALI", pageable);
        assertThat(results).hasSize(1);
        assertThat(results.get(0).getEmail()).isEqualTo("alice@example.com");
    }

    @Test
    void findByEmailContainingIgnoreCase_respectsLimit() {
        Pageable pageable = PageRequest.of(0, 2);
        List<User> results = userRepository.findByEmailContainingIgnoreCase("example", pageable);
        assertThat(results).hasSize(2);
    }

    @Test
    void findByEmailContainingIgnoreCase_returnsEmpty_forNoMatch() {
        Pageable pageable = PageRequest.of(0, 10);
        List<User> results = userRepository.findByEmailContainingIgnoreCase("zzz", pageable);
        assertThat(results).isEmpty();
    }


    @Test
    void existsDirectConversationBetween_returnsTrue_whenExists() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);
        saveParticipant(bob, conv, ParticipantRole.ADMIN);

        assertThat(conversationRepository.existsDirectConversationBetween(
                alice.getId(), bob.getId())).isTrue();
    }

    @Test
    void existsDirectConversationBetween_returnsFalse_whenNoDirectConversation() {
        assertThat(conversationRepository.existsDirectConversationBetween(
                alice.getId(), bob.getId())).isFalse();
    }

    @Test
    void existsDirectConversationBetween_returnsFalse_forGroupConversation() {
        Conversation group = saveConversation(ConversationType.GROUP);
        saveParticipant(alice, group, ParticipantRole.ADMIN);
        saveParticipant(bob, group, ParticipantRole.MEMBER);

        assertThat(conversationRepository.existsDirectConversationBetween(
                alice.getId(), bob.getId())).isFalse();
    }

    @Test
    void existsDirectConversationBetween_returnsFalse_forUnrelatedUsers() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);
        saveParticipant(bob, conv, ParticipantRole.ADMIN);

        assertThat(conversationRepository.existsDirectConversationBetween(
                alice.getId(), carol.getId())).isFalse();
    }


    @Test
    void findAllByConversationId_returnsAllParticipants() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);
        saveParticipant(bob, conv, ParticipantRole.ADMIN);

        List<Participant> result = participantRepository.findAllByConversationId(conv.getId());
        assertThat(result).hasSize(2);
    }

    @Test
    void findAllByConversationId_returnsEmpty_forUnknownConversation() {
        List<Participant> result = participantRepository.findAllByConversationId(9999L);
        assertThat(result).isEmpty();
    }

    @Test
    void existsByConversationIdAndUserEmail_returnsTrue_whenMember() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);

        assertThat(participantRepository.existsByConversationIdAndUserEmail(
                conv.getId(), "alice@example.com")).isTrue();
    }

    @Test
    void existsByConversationIdAndUserEmail_returnsFalse_whenNotMember() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);

        assertThat(participantRepository.existsByConversationIdAndUserEmail(
                conv.getId(), "carol@example.com")).isFalse();
    }

    @Test
    void findAllByUserEmail_returnsPaginatedParticipations() {
        Conversation conv1 = saveConversation(ConversationType.DIRECT);
        Conversation conv2 = saveConversation(ConversationType.GROUP);
        saveParticipant(alice, conv1, ParticipantRole.ADMIN);
        saveParticipant(alice, conv2, ParticipantRole.MEMBER);

        Page<Participant> page = participantRepository.findAllByUserEmail(
                "alice@example.com", PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
    }

    @Test
    void findByConversationIdIn_returnsParticipantsForMultipleConversations() {
        Conversation conv1 = saveConversation(ConversationType.DIRECT);
        Conversation conv2 = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv1, ParticipantRole.ADMIN);
        saveParticipant(bob, conv1, ParticipantRole.ADMIN);
        saveParticipant(alice, conv2, ParticipantRole.ADMIN);
        saveParticipant(carol, conv2, ParticipantRole.ADMIN);

        List<Participant> result = participantRepository.findByConversationIdIn(
                List.of(conv1.getId(), conv2.getId()));

        assertThat(result).hasSize(4);
    }


    @Test
    void findByConversationIdOrderByIdDesc_returnsMessagesDescending() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveParticipant(alice, conv, ParticipantRole.ADMIN);

        Message m1 = saveMessage(conv, alice, "first");
        Message m2 = saveMessage(conv, alice, "second");
        Message m3 = saveMessage(conv, alice, "third");

        List<Message> result = messageRepository.findByConversationIdOrderByIdDesc(
                conv.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(3);
        assertThat(result.get(0).getId()).isGreaterThan(result.get(1).getId());
        assertThat(result.get(1).getId()).isGreaterThan(result.get(2).getId());
    }

    @Test
    void findByConversationIdOrderByIdDesc_respectsPageLimit() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        saveMessage(conv, alice, "msg1");
        saveMessage(conv, alice, "msg2");
        saveMessage(conv, alice, "msg3");

        List<Message> result = messageRepository.findByConversationIdOrderByIdDesc(
                conv.getId(), PageRequest.of(0, 2));

        assertThat(result).hasSize(2);
    }

    @Test
    void findByConversationIdAndIdLessThan_returnsCursorPageCorrectly() {
        Conversation conv = saveConversation(ConversationType.DIRECT);
        Message m1 = saveMessage(conv, alice, "msg1");
        Message m2 = saveMessage(conv, alice, "msg2");
        Message m3 = saveMessage(conv, alice, "msg3");

        List<Message> result = messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(
                conv.getId(), m3.getId(), PageRequest.of(0, 10));

        assertThat(result).hasSize(2);
        assertThat(result).noneMatch(m -> m.getId().equals(m3.getId()));
    }

    @Test
    void findByConversationIdOrderByIdDesc_returnsEmpty_forOtherConversation() {
        Conversation conv1 = saveConversation(ConversationType.DIRECT);
        Conversation conv2 = saveConversation(ConversationType.DIRECT);
        saveMessage(conv1, alice, "msg");

        List<Message> result = messageRepository.findByConversationIdOrderByIdDesc(
                conv2.getId(), PageRequest.of(0, 10));

        assertThat(result).isEmpty();
    }


    @Test
    void findByToken_returnsToken_whenExists() {
        saveRefreshToken(alice, "token-abc", "device-1");

        Optional<RefreshToken> result = refreshTokenRepository.findByToken("token-abc");
        assertThat(result).isPresent();
        assertThat(result.get().getDeviceId()).isEqualTo("device-1");
    }

    @Test
    void findByToken_returnsEmpty_whenNotExists() {
        assertThat(refreshTokenRepository.findByToken("nonexistent")).isEmpty();
    }

    @Test
    void deleteByUserAndDeviceId_removesCorrectToken() {
        saveRefreshToken(alice, "token-1", "device-1");
        saveRefreshToken(alice, "token-2", "device-2");

        refreshTokenRepository.deleteByUserAndDeviceId(alice, "device-1");

        assertThat(refreshTokenRepository.findByToken("token-1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-2")).isPresent();
    }

    @Test
    void deleteByUser_removesAllUserTokens() {
        saveRefreshToken(alice, "token-a1", "device-1");
        saveRefreshToken(alice, "token-a2", "device-2");
        saveRefreshToken(bob,   "token-b1", "device-1");

        refreshTokenRepository.deleteByUser(alice);

        assertThat(refreshTokenRepository.findByToken("token-a1")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-a2")).isEmpty();
        assertThat(refreshTokenRepository.findByToken("token-b1")).isPresent();
    }

    @Test
    void existsByUserAndDeviceId_returnsTrue_whenExists() {
        saveRefreshToken(alice, "token-1", "device-1");
        assertThat(refreshTokenRepository.existsByUserAndDeviceId(alice, "device-1")).isTrue();
    }

    @Test
    void existsByUserAndDeviceId_returnsFalse_whenNotExists() {
        assertThat(refreshTokenRepository.existsByUserAndDeviceId(alice, "device-99")).isFalse();
    }

    

    private User saveUser(String email, String name) {
        return userRepository.save(User.builder()
                .email(email).name(name).password("hashed")
                .frontSalt("salt").publicKey("pubkey").encryptedPrivateKey("encpriv")
                .build());
    }

    private Conversation saveConversation(ConversationType type) {
        Conversation c = new Conversation();
        c.setType(type);
        return conversationRepository.save(c);
    }

    private Participant saveParticipant(User user, Conversation conv, ParticipantRole role) {
        return participantRepository.save(Participant.builder()
                .user(user).conversation(conv)
                .encryptedAesKey("aes-key").role(role).build());
    }

    private Message saveMessage(Conversation conv, User sender, String ciphertext) {
        Message m = new Message();
        m.setConversation(conv);
        m.setSender(sender);
        m.setCiphertext(ciphertext);
        m.setIv("iv");
        m.setTimestamp(LocalDateTime.now());
        return messageRepository.save(m);
    }

    private RefreshToken saveRefreshToken(User user, String token, String deviceId) {
        return refreshTokenRepository.save(RefreshToken.builder()
                .token(token).user(user).deviceId(deviceId)
                .expiryDate(Instant.now().plusSeconds(3600))
                .build());
    }
}
