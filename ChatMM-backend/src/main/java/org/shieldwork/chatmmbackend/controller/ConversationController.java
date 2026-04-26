package org.shieldwork.chatmmbackend.controller;

import org.shieldwork.chatmmbackend.dto.request.CreateConversationRequest;
import org.shieldwork.chatmmbackend.dto.response.ConversationCreatedResponse;
import org.shieldwork.chatmmbackend.dto.response.ConversationSummaryResponse;
import org.shieldwork.chatmmbackend.dto.response.MessagePageResponse;
import org.shieldwork.chatmmbackend.dto.response.PublicKeyResponse;
import org.shieldwork.chatmmbackend.service.ChatMessageService;
import org.shieldwork.chatmmbackend.service.ConversationService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/conversations")
@RequiredArgsConstructor
public class ConversationController {

    private final ChatMessageService chatMessageService;
    private final ConversationService conversationService;

    @PostMapping
    public ResponseEntity<ConversationCreatedResponse> createConversation(
            @Valid @RequestBody CreateConversationRequest request,
            Authentication authentication
    ) {
        String creatorEmail = authentication.getName();

        Long newConversationId = conversationService.createConversation(request, creatorEmail);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(new ConversationCreatedResponse(newConversationId));
    }

    @GetMapping("/{conversationId}/keys")
    public ResponseEntity<List<PublicKeyResponse>> getParticipantPublicKeys(
            @PathVariable Long conversationId,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        List<PublicKeyResponse> keys = conversationService.getParticipantPublicKeys(conversationId, userEmail);

        return ResponseEntity.ok(keys);
    }

    // cursor pagination
    // @Min @Max for limit ?
    @GetMapping("/{conversationId}/messages")
    public ResponseEntity<MessagePageResponse> getMessages(
            @PathVariable Long conversationId,
            @RequestParam(required = false) Long cursor,
            @RequestParam(defaultValue = "50") int limit,
            Authentication authentication
    ) {
        String userEmail = authentication.getName();

        MessagePageResponse response = chatMessageService.getConversationMessages(conversationId, userEmail, cursor, limit);

        return ResponseEntity.ok(response);
    }

    @GetMapping
    public ResponseEntity<List<ConversationSummaryResponse>> getUserConversations(
            Authentication authentication
    ) {
        String userEmail = authentication.getName();
        List<ConversationSummaryResponse> conversations = conversationService.getUserConversations(userEmail);
        return ResponseEntity.ok(conversations);
    }
}