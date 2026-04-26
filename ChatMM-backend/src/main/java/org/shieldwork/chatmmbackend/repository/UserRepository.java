package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import java.util.Optional;

public interface UserRepository extends JpaRepository<User, Long> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);

//    List<User> findTop10ByEmailContainingIgnoreCase(String emailPhrase);
}
