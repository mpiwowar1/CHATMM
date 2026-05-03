package org.shieldwork.chatmmbackend.model;

import jakarta.persistence.*;
import lombok.*;
import org.shieldwork.chatmmbackend.model.enums.ParticipantRole;

import java.time.LocalDateTime;

@Entity
@Table(name = "participants")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Participant {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "conversation_id", nullable = false)
    private Conversation conversation;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String encryptedAesKey;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private ParticipantRole role;

    @Column(nullable = false, updatable = false)
    private LocalDateTime joinedAt;

    @PrePersist
    protected void onCreate() {
        this.joinedAt = LocalDateTime.now();
        // Depends on DTO
        // if (this.role == null) this.role = ParticipantRole.MEMBER;
    }
}

