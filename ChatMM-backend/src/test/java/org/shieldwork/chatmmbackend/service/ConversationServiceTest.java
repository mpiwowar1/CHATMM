package org.shieldwork.chatmmbackend.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;

import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ConversationServiceTest {

    @Mock private ConversationRepository conversationRepository;
    @Mock private ParticipantRepository participantRepository;
    @Mock private UserRepository userRepository;

    @InjectMocks
    private ConversationService conversationService;

    @Test
    void createConversation_ShouldCreateDirectConversation_WhenTwoParticipantsProvided() {
        String creatorEmail = "creator@example.com";
        User creator = User.builder().id(1L).email(creatorEmail).build();
        User friend = User.builder().id(2L).email("friend@example.com").build();

        CreateConversationRequest request = new CreateConversationRequest();
        request.setName("Direct Chat");
        request.setParticipantKeys(Map.of(
                1L, "encrypted_key_for_creator",
                2L, "encrypted_key_for_friend"
        ));

        when(userRepository.findByEmail(creatorEmail)).thenReturn(Optional.of(creator));

        Conversation savedConversation = Conversation.builder().id(100L).type(ConversationType.DIRECT).build();
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        when(userRepository.findAllById(Set.of(1L, 2L))).thenReturn(List.of(creator, friend));

        Long resultId = conversationService.createConversation(request, creatorEmail);

        assertEquals(100L, resultId, "Should return ID of the newly created conversation");

        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convCaptor.capture());
        assertEquals(ConversationType.DIRECT, convCaptor.getValue().getType(), "Type should be DIRECT for 2 users");
        assertEquals("Direct Chat", convCaptor.getValue().getName());

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Participant>> partCaptor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(partCaptor.capture());

        List<Participant> savedParticipants = partCaptor.getValue();
        assertEquals(2, savedParticipants.size(), "Should save 2 participants");

        assertTrue(savedParticipants.stream().allMatch(p -> p.getRole() == ParticipantRole.ADMIN), "In DIRECT chat, both should be ADMIN");
    }

    @Test
    void createConversation_ShouldCreateGroupConversation_WhenMoreThanTwoParticipantsProvided() {
        String creatorEmail = "creator@example.com";
        User creator = User.builder().id(1L).email(creatorEmail).build();
        User friend1 = User.builder().id(2L).build();
        User friend2 = User.builder().id(3L).build();

        CreateConversationRequest request = new CreateConversationRequest();
        request.setName("Group Chat");
        request.setParticipantKeys(Map.of(
                1L, "key1",
                2L, "key2",
                3L, "key3"
        ));

        when(userRepository.findByEmail(creatorEmail)).thenReturn(Optional.of(creator));

        Conversation savedConversation = Conversation.builder().id(100L).type(ConversationType.GROUP).build();
        when(conversationRepository.save(any(Conversation.class))).thenReturn(savedConversation);

        when(userRepository.findAllById(Set.of(1L, 2L, 3L))).thenReturn(List.of(creator, friend1, friend2));

        conversationService.createConversation(request, creatorEmail);

        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationRepository).save(convCaptor.capture());
        assertEquals(ConversationType.GROUP, convCaptor.getValue().getType(), "Type should be GROUP for >2 users");

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<Participant>> partCaptor = ArgumentCaptor.forClass(List.class);
        verify(participantRepository).saveAll(partCaptor.capture());

        List<Participant> savedParticipants = partCaptor.getValue();

        for (Participant p : savedParticipants) {
            if (p.getUser().getId().equals(1L)) {
                assertEquals(ParticipantRole.ADMIN, p.getRole(), "Creator should be ADMIN in a GROUP");
            } else {
                assertEquals(ParticipantRole.MEMBER, p.getRole(), "Invited users should be MEMBER in a GROUP");
            }
        }
    }

    @Test
    void createConversation_ShouldThrowIllegalArgumentException_WhenCreatorIsNotInParticipantList() {
        String creatorEmail = "creator@example.com";
        User creator = User.builder().id(1L).email(creatorEmail).build();

        CreateConversationRequest request = new CreateConversationRequest();
        request.setParticipantKeys(Map.of(
                2L, "key2",
                3L, "key3"
        ));

        when(userRepository.findByEmail(creatorEmail)).thenReturn(Optional.of(creator));

        IllegalArgumentException exception = assertThrows(
                IllegalArgumentException.class,
                () -> conversationService.createConversation(request, creatorEmail)
        );

        assertEquals("Conversation creator must be a participant", exception.getMessage());

        verify(conversationRepository, never()).save(any(Conversation.class));
        verify(participantRepository, never()).saveAll(anyIterable());
    }

    @Test
    void createConversation_ShouldThrowResourceNotFoundException_WhenAnyRequestedUserDoesNotExist() {
        String creatorEmail = "creator@example.com";
        User creator = User.builder().id(1L).email(creatorEmail).build();

        CreateConversationRequest request = new CreateConversationRequest();
        request.setParticipantKeys(Map.of(
                1L, "key1",
                99L, "key_for_nonexistent_user"
        ));

        when(userRepository.findByEmail(creatorEmail)).thenReturn(Optional.of(creator));
        when(conversationRepository.save(any(Conversation.class))).thenReturn(new Conversation());

        when(userRepository.findAllById(Set.of(1L, 99L))).thenReturn(List.of(creator));

        ResourceNotFoundException exception = assertThrows(
                ResourceNotFoundException.class,
                () -> conversationService.createConversation(request, creatorEmail)
        );

        assertEquals("One or more of the provided user IDs do not exist", exception.getMessage());

        verify(participantRepository, never()).saveAll(anyIterable());
    }
}