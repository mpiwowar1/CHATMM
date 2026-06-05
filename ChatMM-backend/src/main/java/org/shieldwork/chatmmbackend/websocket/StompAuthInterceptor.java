package org.shieldwork.chatmmbackend.websocket;

import lombok.NonNull;
import lombok.RequiredArgsConstructor;
import org.shieldwork.chatmmbackend.repository.ParticipantRepository;
import org.shieldwork.chatmmbackend.security.JwtService;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.AuthenticationCredentialsNotFoundException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class StompAuthInterceptor implements ChannelInterceptor {

    private final JwtService jwtService;
    private final UserDetailsService userDetailsService;
    private final ParticipantRepository participantRepository;

    @Override
    public Message<?> preSend(@NonNull Message<?> message, @NonNull MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null || accessor.getCommand() == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            handleConnect(accessor);
        } else if (StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            handleSubscribe(accessor);
        }   else if (StompCommand.SEND.equals(accessor.getCommand())) {
            handleSend(accessor);
        }

        return message;
    }

    private void handleConnect(StompHeaderAccessor accessor) {
        String authHeader = accessor.getFirstNativeHeader("Authorization");
        if (authHeader != null && authHeader.startsWith("Bearer ")) {
            String token = authHeader.substring(7);
            String userEmail = jwtService.extractUsername(token);

            if (userEmail != null) {
                UserDetails userDetails = userDetailsService.loadUserByUsername(userEmail);
                if (jwtService.isTokenValid(token, userDetails)) {
                    UsernamePasswordAuthenticationToken authentication =
                            new UsernamePasswordAuthenticationToken(userDetails, null, userDetails.getAuthorities());
                    accessor.setUser(authentication);
                } else {
                    throw new BadCredentialsException("Invalid JWT token");
                }
            }
        } else {
            throw new AuthenticationCredentialsNotFoundException("Missing JWT token in WebSocket header");
        }
    }

    private void handleSubscribe(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination != null && destination.startsWith("/topic/conversation.")) {
            try {
                String convIdStr = destination.replace("/topic/conversation.", "");
                Long conversationId = Long.parseLong(convIdStr);

                if (accessor.getUser() == null) {
                    throw new AuthenticationException("Authentication missing") {};
                }
                String userEmail = accessor.getUser().getName();

                boolean isMember = participantRepository.existsByConversationIdAndUserEmail(conversationId, userEmail);

                if (!isMember) {
                    throw new AccessDeniedException("Access denied to this channel");
                }

            } catch (NumberFormatException e) {
                throw new IllegalArgumentException("Invalid conversation ID format.");
            }
        }
    }

    private void handleSend(StompHeaderAccessor accessor) {
        String destination = accessor.getDestination();

        if (destination != null && !destination.startsWith("/app/")) {
            throw new AccessDeniedException("Direct messaging to the broker is forbidden. Use /app/ endpoints.");
        }
    }
}