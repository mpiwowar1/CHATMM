package org.shieldwork.chatmmbackend.service;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.dto.response.ConversationSummaryResponse;
import org.shieldwork.chatmmbackend.dto.response.PublicKeyResponse;
import org.shieldwork.chatmmbackend.exception.ConversationAlreadyExistsException;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.access.AccessDeniedException;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock ConversationRepository conversationRepository;
    @Mock ParticipantRepository participantRepository;
    @Mock UserRepository userRepository;
    @Mock NotificationService notificationService;

    @InjectMocks ConversationService conversationService;


    @Test
    void createConversation_createsDirect_forTwoParticipants() {
        User creator = buildUser(1L, "alice@example.com");
        User other   = buildUser(2L, "bob@example.com");

        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(1L, "key1", 2L, "key2"));

        Conversation saved = buildConversation(10L, ConversationType.DIRECT);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));
        when(conversationRepository.existsDirectConversationBetween(anyLong(), anyLong())).thenReturn(false);
        when(conversationRepository.save(any())).thenReturn(saved);
        when(userRepository.findAllById(any())).thenReturn(List.of(creator, other));
        when(participantRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        Long id = conversationService.createConversation(req, "alice@example.com");

        assertThat(id).isEqualTo(10L);
        verify(conversationRepository).save(argThat(c -> c.getType() == ConversationType.DIRECT));
    }

    @Test
    void createConversation_createsGroup_forThreeParticipants() {
        User creator = buildUser(1L, "alice@example.com");
        User bob     = buildUser(2L, "bob@example.com");
        User carol   = buildUser(3L, "carol@example.com");

        CreateConversationRequest req = new CreateConversationRequest();
        req.setName("Group Chat");
        req.setParticipantKeys(Map.of(1L, "k1", 2L, "k2", 3L, "k3"));

        Conversation saved = buildConversation(20L, ConversationType.GROUP);
        saved.setName("Group Chat");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));
        when(conversationRepository.save(any())).thenReturn(saved);
        when(userRepository.findAllById(any())).thenReturn(List.of(creator, bob, carol));
        when(participantRepository.saveAll(any())).thenAnswer(inv -> inv.getArgument(0));

        Long id = conversationService.createConversation(req, "alice@example.com");

        assertThat(id).isEqualTo(20L);
        verify(conversationRepository).save(argThat(c -> c.getType() == ConversationType.GROUP));
    }

    @Test
    void createConversation_throws_whenCreatorNotParticipant() {
        User creator = buildUser(1L, "alice@example.com");
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(2L, "k2", 3L, "k3")); 

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> conversationService.createConversation(req, "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("creator must be a participant");
    }

    @Test
    void createConversation_throws_whenOnlyOneParticipant() {
        User creator = buildUser(1L, "alice@example.com");
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(1L, "k1"));

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));

        assertThatThrownBy(() -> conversationService.createConversation(req, "alice@example.com"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("at least two");
    }

    @Test
    void createConversation_throws_whenDirectAlreadyExists() {
        User creator = buildUser(1L, "alice@example.com");
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(1L, "k1", 2L, "k2"));

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));
        when(conversationRepository.existsDirectConversationBetween(anyLong(), anyLong())).thenReturn(true);

        assertThatThrownBy(() -> conversationService.createConversation(req, "alice@example.com"))
                .isInstanceOf(ConversationAlreadyExistsException.class);
    }

    @Test
    void createConversation_throws_whenUserIdNotFound() {
        User creator = buildUser(1L, "alice@example.com");
        CreateConversationRequest req = new CreateConversationRequest();
        req.setParticipantKeys(Map.of(1L, "k1", 99L, "k99")); 

        Conversation saved = buildConversation(10L, ConversationType.DIRECT);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(creator));
        when(conversationRepository.existsDirectConversationBetween(anyLong(), anyLong())).thenReturn(false);
        when(conversationRepository.save(any())).thenReturn(saved);
        when(userRepository.findAllById(any())).thenReturn(List.of(creator)); 

        assertThatThrownBy(() -> conversationService.createConversation(req, "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }


    @Test
    void getParticipantPublicKeys_returnsOtherParticipantsKeys() {
        User alice = buildUser(1L, "alice@example.com");
        User bob   = buildUser(2L, "bob@example.com");

        Participant pAlice = buildParticipant(alice, 10L);
        Participant pBob   = buildParticipant(bob, 10L);

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.findAllByConversationId(10L)).thenReturn(List.of(pAlice, pBob));

        List<PublicKeyResponse> keys = conversationService.getParticipantPublicKeys(10L, "alice@example.com");

        assertThat(keys).hasSize(1);
        assertThat(keys.get(0).getUserId()).isEqualTo(2L);
        assertThat(keys.get(0).getPublicKey()).isEqualTo("pubkey");
    }

    @Test
    void getParticipantPublicKeys_throws_whenConversationNotFound() {
        User alice = buildUser(1L, "alice@example.com");
        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(conversationRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> conversationService.getParticipantPublicKeys(99L, "alice@example.com"))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void getParticipantPublicKeys_throws_whenNotMember() {
        User alice = buildUser(1L, "alice@example.com");
        User bob   = buildUser(2L, "bob@example.com");

        when(userRepository.findByEmail("alice@example.com")).thenReturn(Optional.of(alice));
        when(conversationRepository.existsById(10L)).thenReturn(true);
        when(participantRepository.findAllByConversationId(10L)).thenReturn(List.of(buildParticipant(bob, 10L)));

        assertThatThrownBy(() -> conversationService.getParticipantPublicKeys(10L, "alice@example.com"))
                .isInstanceOf(AccessDeniedException.class);
    }


    @Test
    void getUserConversations_returnsEmpty_whenNoConversations() {
        when(participantRepository.findAllByUserEmail(eq("alice@example.com"), any()))
                .thenReturn(Page.empty());

        Page<ConversationSummaryResponse> result = conversationService.getUserConversations("alice@example.com", 0, 10);

        assertThat(result).isEmpty();
    }

    @Test
    void getUserConversations_returnsDirectConversation_withOtherUserNameAsDisplayName() {
        User alice = buildUser(1L, "alice@example.com");
        User bob   = buildUser(2L, "bob@example.com");

        Conversation conv = buildConversation(10L, ConversationType.DIRECT);

        Participant myPart  = buildParticipantFull(alice, conv);
        Participant bobPart = buildParticipantFull(bob, conv);

        Page<Participant> page = new PageImpl<>(List.of(myPart), PageRequest.of(0, 10), 1);

        when(participantRepository.findAllByUserEmail(eq("alice@example.com"), any())).thenReturn(page);
        when(participantRepository.findByConversationIdIn(List.of(10L))).thenReturn(List.of(myPart, bobPart));

        Page<ConversationSummaryResponse> result = conversationService.getUserConversations("alice@example.com", 0, 10);

        assertThat(result.getContent()).hasSize(1);
        assertThat(result.getContent().get(0).getName()).isEqualTo("Bob"); 
    }


    private User buildUser(Long id, String email) {
        String firstName = email.split("@")[0];
        String name = firstName.substring(0, 1).toUpperCase() + firstName.substring(1);
        return User.builder().id(id).email(email).name(name)
                .password("p").frontSalt("s").publicKey("pubkey").encryptedPrivateKey("e").build();
    }

    private Conversation buildConversation(Long id, ConversationType type) {
        Conversation c = new Conversation();
        c.setId(id);
        c.setType(type);
        return c;
    }

    private Participant buildParticipant(User user, Long conversationId) {
        Conversation conv = new Conversation();
        conv.setId(conversationId);
        conv.setType(ConversationType.DIRECT);
        return Participant.builder()
                .user(user)
                .conversation(conv)
                .encryptedAesKey("aes-key")
                .role(ParticipantRole.ADMIN)
                .build();
    }

    private Participant buildParticipantFull(User user, Conversation conv) {
        return Participant.builder()
                .user(user)
                .conversation(conv)
                .encryptedAesKey("aes-key")
                .role(ParticipantRole.ADMIN)
                .build();
    }
}
