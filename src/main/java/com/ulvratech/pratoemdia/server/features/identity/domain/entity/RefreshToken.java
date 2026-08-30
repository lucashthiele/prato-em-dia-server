package com.ulvratech.pratoemdia.server.features.identity.domain.entity;

import java.time.Instant;
import java.util.UUID;

public class RefreshToken {
    private final UUID id;
    private final UUID userAccountId;
    private final String tokenHash;
    private final Instant expiresAt;
    private final Instant createdAt;
    private Instant revokedAt;
    private UUID replacedByTokenId;

    public RefreshToken(
        UUID id,
        UUID userAccountId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt,
        Instant revokedAt,
        UUID replacedByTokenId
    ) {
        this.id = id;
        this.userAccountId = userAccountId;
        this.tokenHash = tokenHash;
        this.expiresAt = expiresAt;
        this.createdAt = createdAt;
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public static RefreshToken issue(
        UUID id,
        UUID userAccountId,
        String tokenHash,
        Instant expiresAt,
        Instant createdAt
    ) {
        return new RefreshToken(
            id,
            userAccountId,
            tokenHash,
            expiresAt,
            createdAt,
            null,
            null
        );
    }

    public boolean isExpiredAt(Instant instant) {
        return !expiresAt.isAfter(instant);
    }

    public boolean isRevoked() {
        return revokedAt != null;
    }

    public void revoke(Instant revokedAt, UUID replacedByTokenId) {
        if (isRevoked()) {
            throw new IllegalStateException("Refresh token has already been revoked");
        }
        this.revokedAt = revokedAt;
        this.replacedByTokenId = replacedByTokenId;
    }

    public UUID getId() {
        return id;
    }

    public UUID getUserAccountId() {
        return userAccountId;
    }

    public String getTokenHash() {
        return tokenHash;
    }

    public Instant getExpiresAt() {
        return expiresAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getRevokedAt() {
        return revokedAt;
    }

    public UUID getReplacedByTokenId() {
        return replacedByTokenId;
    }
}
