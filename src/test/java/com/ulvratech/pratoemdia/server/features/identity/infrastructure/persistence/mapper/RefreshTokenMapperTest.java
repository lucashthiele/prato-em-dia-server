package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.mapper;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.RefreshTokenModel;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;

class RefreshTokenMapperTest {
  @Test
  void mapsRefreshTokenInBothDirections() {
    var id = UUID.randomUUID();
    var account = UUID.randomUUID();
    var replacement = UUID.randomUUID();
    var created = Instant.parse("2026-09-11T10:00:00Z");
    var expires = created.plusSeconds(3600);
    var revoked = created.plusSeconds(60);
    var model =
        RefreshTokenModel.builder()
            .id(id)
            .userAccountId(account)
            .tokenHash("hash")
            .createdAt(created)
            .expiresAt(expires)
            .revokedAt(revoked)
            .replacedByTokenId(replacement)
            .build();
    var mapper = new RefreshTokenMapper();

    var entity = mapper.toEntity(model);
    var roundTrip = mapper.toModel(entity);

    assertEquals(id, roundTrip.getId());
    assertEquals(account, roundTrip.getUserAccountId());
    assertEquals("hash", roundTrip.getTokenHash());
    assertEquals(expires, roundTrip.getExpiresAt());
    assertEquals(created, roundTrip.getCreatedAt());
    assertEquals(revoked, roundTrip.getRevokedAt());
    assertEquals(replacement, roundTrip.getReplacedByTokenId());
  }
}
