package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {
}