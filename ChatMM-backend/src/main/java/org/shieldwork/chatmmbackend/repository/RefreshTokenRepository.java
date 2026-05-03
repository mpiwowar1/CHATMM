package org.shieldwork.chatmmbackend.repository;

import org.shieldwork.chatmmbackend.model.RefreshToken;
import org.shieldwork.chatmmbackend.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    Optional<RefreshToken> findByToken(String token);

    void deleteByUser(User user);

    void deleteByUserAndDeviceId(User user, String deviceId);

    boolean existsByUserAndDeviceId(User user, String deviceId);
}
