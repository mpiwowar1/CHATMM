package org.shieldwork.chatmmbackend.model;

import jakarta.persistence.*;
import lombok.*;
import org.shieldwork.chatmmbackend.model.enums.ConversationType;

import java.time.LocalDateTime;

@Entity
@Table(name = "conversations")
@Setter
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Conversation {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;

    @Enumerated(EnumType.STRING)
//    @Column(nullable = false)
    private ConversationType type;

    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    protected void onCreate() {
        this.createdAt = LocalDateTime.now();
    }
}
