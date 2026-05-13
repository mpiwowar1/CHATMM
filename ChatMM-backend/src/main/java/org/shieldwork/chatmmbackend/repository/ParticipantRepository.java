package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.Participant;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Participant> findAllByConversationId(Long conversationId);

    @EntityGraph(attributePaths = {"user", "conversation"})
    List<Participant> findByConversationIdIn(List<Long> conversationIds);

    boolean existsByConversationIdAndUserEmail(Long conversationId, String email);

    @EntityGraph(attributePaths = {"conversation"})
    Page<Participant> findAllByUserEmail(String email, Pageable pageable);
}
