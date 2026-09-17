package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.mapper.RefreshTokenMapper;
import java.time.Instant;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Repository;

@Repository
public class RefreshTokenRepositoryAdapter implements RefreshTokenRepository {
  private final RefreshTokenJpaRepository repository;
  private final RefreshTokenMapper mapper;

  public RefreshTokenRepositoryAdapter(
      RefreshTokenJpaRepository repository, RefreshTokenMapper mapper) {
    this.repository = repository;
    this.mapper = mapper;
  }

  @Override
  public RefreshToken save(RefreshToken refreshToken) {
    return mapper.toEntity(repository.save(mapper.toModel(refreshToken)));
  }

  @Override
  public Optional<RefreshToken> findByTokenHashForUpdate(String tokenHash) {
    return repository.findByTokenHashForUpdate(tokenHash).map(mapper::toEntity);
  }

  @Override
  public void revokeAllByUserAccountId(UUID userAccountId, Instant revokedAt) {
    repository.revokeAllByUserAccountId(userAccountId, revokedAt);
  }
}
