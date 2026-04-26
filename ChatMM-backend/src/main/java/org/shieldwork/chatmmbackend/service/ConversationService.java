package org.shieldwork.chatmmbackend.service;

import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.dto.response.ConversationSummaryResponse;
import org.shieldwork.chatmmbackend.dto.response.PublicKeyResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Participant;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class ConversationService {

    private final ConversationRepository conversationRepository;
    private final ParticipantRepository participantRepository;
    private final UserRepository userRepository;

    @Transactional
    public Long createConversation(CreateConversationRequest request, String creatorEmail) {

        User creator = userRepository.findByEmail(creatorEmail)
                .orElseThrow(() -> new ResourceNotFoundException("Conversation creator not found"));

        Map<Long, String> participantKeys = request.getParticipantKeys();
        Set<Long> requestedUserIds = participantKeys.keySet();

        if (!requestedUserIds.contains(creator.getId())) {
            throw new IllegalArgumentException("Conversation creator must be a participant");
        }

        ConversationType type = requestedUserIds.size() > 2 ? ConversationType.GROUP : ConversationType.DIRECT;

        Conversation conversation = Conversation.builder()
                .name(request.getName())
                .type(type)
                .build();

        Conversation savedConversation = conversationRepository.save(conversation);

        List<User> participantUsers = userRepository.findAllById(requestedUserIds);

        if (participantUsers.size() != requestedUserIds.size()) {
            throw new ResourceNotFoundException("One or more of the provided user IDs do not exist");
        }

        List<Participant> participantsToSave = participantUsers.stream().map(user -> {
            String encryptedAesKey = participantKeys.get(user.getId());

            ParticipantRole role;
            if (type == ConversationType.DIRECT) {
                role = ParticipantRole.ADMIN;
            } else {
                role = user.getId().equals(creator.getId()) ? ParticipantRole.ADMIN : ParticipantRole.MEMBER;
            }

            return Participant.builder()
                    .conversation(savedConversation)
                    .user(user)
                    .encryptedAesKey(encryptedAesKey)
                    .role(role)
                    .build();
        }).toList();

        participantRepository.saveAll(participantsToSave);

        return savedConversation.getId();
    }


    @Transactional(readOnly = true)
    public List<PublicKeyResponse> getParticipantPublicKeys(Long conversationId, String requesterEmail) {

        User requester = userRepository.findByEmail(requesterEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        List<Participant> participants = participantRepository.findAllByConversationId(conversationId);

        boolean isMember = participants.stream()
                .anyMatch(p -> p.getUser().getId().equals(requester.getId()));

        if (!isMember) {
            throw new SecurityException("You are not a member of this conversation.");
        }

        return participants.stream()
                .filter(p -> !p.getUser().getId().equals(requester.getId()))
                .map(p -> PublicKeyResponse.builder()
                        .userId(p.getUser().getId())
                        .publicKey(p.getUser().getPublicKey())
                        .build())
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ConversationSummaryResponse> getUserConversations(String email) {

        List<Participant> participations = participantRepository.findAllByUserEmail(email);

        return participations.stream()
                .map(p -> ConversationSummaryResponse.builder()
                        .id(p.getConversation().getId())
                        .name(p.getConversation().getName())
                        .type(p.getConversation().getType())
                        .build())
                .toList();
    }
}