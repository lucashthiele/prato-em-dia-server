package com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.infrastructure.persistence.model.RefreshTokenModel;
import org.springframework.stereotype.Component;

@Component
public class RefreshTokenMapper {
    public RefreshToken toEntity(RefreshTokenModel model) {
        return new RefreshToken(
                model.getId(),
                model.getUserAccountId(),
                model.getTokenHash(),
                model.getExpiresAt(),
                model.getCreatedAt(),
                model.getRevokedAt(),
                model.getReplacedByTokenId()
        );
    }

    public RefreshTokenModel toModel(RefreshToken entity) {
        return RefreshTokenModel.builder()
                .id(entity.getId())
                .userAccountId(entity.getUserAccountId())
                .tokenHash(entity.getTokenHash())
                .expiresAt(entity.getExpiresAt())
                .createdAt(entity.getCreatedAt())
                .revokedAt(entity.getRevokedAt())
                .replacedByTokenId(entity.getReplacedByTokenId())
                .build();
    }
}
