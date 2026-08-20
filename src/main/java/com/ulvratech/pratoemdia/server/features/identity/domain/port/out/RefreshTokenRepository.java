package com.ulvratech.pratoemdia.server.features.identity.domain.port.out;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;

import java.util.Optional;

public interface RefreshTokenRepository {
    RefreshToken save(RefreshToken refreshToken);

    Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash);
}
