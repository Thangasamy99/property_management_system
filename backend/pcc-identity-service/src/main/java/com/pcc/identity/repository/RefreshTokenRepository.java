package com.pcc.identity.repository;

import com.pcc.identity.entity.RefreshToken;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {

    @EntityGraph(attributePaths = {"user", "user.roles", "user.roles.permissions"})
    Optional<RefreshToken> findByTokenHash(String tokenHash);

    @Modifying
    @Query("UPDATE RefreshToken t SET t.revoked = true, t.revokedAt = CURRENT_TIMESTAMP WHERE t.user.id = :userId AND t.revoked = false")
    int revokeAllActiveByUserId(@Param("userId") Long userId);
}
