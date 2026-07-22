package com.wida3.auth.repository;

import com.wida3.auth.entity.RefreshToken;
import com.wida3.auth.entity.User;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    List<RefreshToken> findAllByUserAndRevokedAtIsNull(User user);
}
