package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.Message;
import org.springframework.data.domain.Pageable;
import org.shieldwork.chatmmbackend.model.Message;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

// N+1 Query
public interface MessageRepository extends JpaRepository<Message, Long> {

    @EntityGraph(attributePaths = {"sender"})
    List<Message> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);
    @EntityGraph(attributePaths = {"sender"})
    List<Message> findByConversationIdAndIdLessThanOrderByIdDesc(Long conversationId, Long cursorId, Pageable pageable);
}