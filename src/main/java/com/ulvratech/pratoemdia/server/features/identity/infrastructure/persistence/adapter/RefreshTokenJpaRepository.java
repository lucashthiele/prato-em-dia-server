package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.RefreshTokenModel;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RefreshTokenJpaRepository extends JpaRepository<RefreshTokenModel, UUID> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select token from RefreshTokenModel token where token.tokenHash = :tokenHash")
    Optional<RefreshTokenModel> findByTokenHashForUpdate(
        @Param("tokenHash") String tokenHash
    );
}
