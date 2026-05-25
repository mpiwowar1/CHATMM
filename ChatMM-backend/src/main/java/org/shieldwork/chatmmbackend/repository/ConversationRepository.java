package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface ConversationRepository extends JpaRepository<Conversation, Long> {

    @Query("SELECT COUNT(c) > 0 FROM Conversation c " +
            "JOIN Participant p1 ON c = p1.conversation " +
            "JOIN Participant p2 ON c = p2.conversation " +
            "WHERE c.type = 'DIRECT' " +
            "AND p1.user.id = :user1Id " +
            "AND p2.user.id = :user2Id")
    boolean existsDirectConversationBetween(@Param("user1Id") Long user1Id, @Param("user2Id") Long user2Id);
}