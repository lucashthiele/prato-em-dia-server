package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.RefreshTokenModel;
import jakarta.persistence.LockModeType;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenModel, UUID> {
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query("select token from RefreshTokenModel token where token.tokenHash = :tokenHash")
  Optional<RefreshTokenModel> findByTokenHashForUpdate(@Param("tokenHash") String tokenHash);

  @Modifying
  @Query(
      "update RefreshTokenModel token set token.revokedAt = :revokedAt where"
          + " token.userAccountId = :userAccountId and token.revokedAt is null")
  int revokeAllByUserAccountId(
      @Param("userAccountId") UUID userAccountId, @Param("revokedAt") Instant revokedAt);
}
