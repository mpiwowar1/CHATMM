package org.shieldwork.chatmmbackend.service;

import org.shieldwork.chatmmbackend.dto.response.MessagePageResponse;
import org.shieldwork.chatmmbackend.dto.request.ChatMessagePayload;
import org.shieldwork.chatmmbackend.dto.response.ChatMessageResponse;
import org.shieldwork.chatmmbackend.exception.ResourceNotFoundException;
import org.shieldwork.chatmmbackend.model.Conversation;
import org.shieldwork.chatmmbackend.model.Message;
import org.shieldwork.chatmmbackend.model.User;
import org.shieldwork.chatmmbackend.repository.ConversationRepository;
import org.shieldwork.chatmmbackend.repository.MessageRepository;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
public class ChatMessageService {

    private final MessageRepository messageRepository;
    private final ConversationRepository conversationRepository;
    private final UserRepository userRepository;
    private final ParticipantRepository participantRepository;

    @Transactional
    public ChatMessageResponse processAndSaveMessage(ChatMessagePayload payload, String senderEmail) {

        User sender = userRepository.findByEmail(senderEmail)
                .orElseThrow(() -> new ResourceNotFoundException("User not found."));

        Conversation conversation = conversationRepository.findById(payload.getConversationId())
                .orElseThrow(() -> new ResourceNotFoundException("Conversation not found"));

        boolean isMember = participantRepository.existsByConversationIdAndUserEmail(conversation.getId(), senderEmail);

        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this conversation.");
        }

        Message message = Message.builder()
                .conversation(conversation)
                .sender(sender)
                .ciphertext(payload.getCiphertext())
                .iv(payload.getIv())
                .build();

        Message savedMessage = messageRepository.save(message);

        conversation.setLastMessageAt(savedMessage.getTimestamp());
        conversation.setLastMessagePreview(payload.getCiphertext());
        conversation.setLastMessageIv(payload.getIv());
        conversation.setLastMessageSenderName(sender.getName());
        conversationRepository.save(conversation);

        return ChatMessageResponse.builder()
                .id(savedMessage.getId())
                .conversationId(conversation.getId())
                .senderId(sender.getId())
                .senderName(sender.getName())
                .ciphertext(savedMessage.getCiphertext())
                .iv(savedMessage.getIv())
                .timestamp(savedMessage.getTimestamp())
                .build();
    }

    @Transactional(readOnly = true)
    public MessagePageResponse getConversationMessages(Long conversationId, String userEmail, Long cursor, int limit) {
        if (!conversationRepository.existsById(conversationId)) {
            throw new ResourceNotFoundException("Conversation not found with id: " + conversationId);
        }

        boolean isMember = participantRepository.existsByConversationIdAndUserEmail(conversationId, userEmail);

        if (!isMember) {
            throw new AccessDeniedException("You are not a member of this conversation.");
        }

        List<Message> messages;
        Pageable pageable = PageRequest.of(0, limit);

        if (cursor == null) {
            messages = messageRepository.findByConversationIdOrderByIdDesc(conversationId, pageable);
        } else {
            messages = messageRepository.findByConversationIdAndIdLessThanOrderByIdDesc(conversationId, cursor, pageable);
        }

        if (messages.isEmpty()) {
            return MessagePageResponse.builder()
                    .messages(List.of())
                    .nextCursor(null)
                    .hasMore(false)
                    .build();
        }

        boolean hasMore = messages.size() == limit;
        Long nextCursor = messages.get(messages.size() - 1).getId();
        
        List<ChatMessageResponse> dtos = messages.stream()
                .sorted(Comparator.comparing(Message::getId)) 
                .map(msg -> ChatMessageResponse.builder()
                        .id(msg.getId())
                        .conversationId(conversationId)
                        .senderId(msg.getSender().getId())
                        .senderName(msg.getSender().getName())
                        .ciphertext(msg.getCiphertext())
                        .iv(msg.getIv())
                        .timestamp(msg.getTimestamp())
                        .build()
                ).toList();

        return MessagePageResponse.builder()
                .messages(dtos)
                .nextCursor(nextCursor)
                .hasMore(hasMore)
                .build();
    }
}
