package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.Participant;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;

public interface ParticipantRepository extends JpaRepository<Participant, Long> {
    @EntityGraph(attributePaths = {"user"})
    List<Participant> findAllByConversationId(Long conversationId);

    boolean existsByConversationIdAndUserEmail(Long conversationId, String email);

    @EntityGraph(attributePaths = {"conversation"})
    List<Participant> findAllByUserEmail(String email);
}
