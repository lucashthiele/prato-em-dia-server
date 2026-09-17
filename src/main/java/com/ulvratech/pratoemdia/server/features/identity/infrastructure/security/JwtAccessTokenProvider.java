package com.ulvratech.pratoemdia.server.features.identity.infrastructure.security;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.GeneratedAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.config.JwtConfigurationProperties;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;

import org.springframework.security.oauth2.jose.jws.MacAlgorithm;
import org.springframework.security.oauth2.jwt.JwsHeader;
import org.springframework.security.oauth2.jwt.JwtClaimsSet;
import org.springframework.security.oauth2.jwt.JwtEncoder;
import org.springframework.security.oauth2.jwt.JwtEncoderParameters;

import java.time.Clock;
import java.util.UUID;

public class JwtAccessTokenProvider implements AccessTokenProvider {
    private final JwtEncoder jwtEncoder;
    private final JwtConfigurationProperties properties;
    private final Clock clock;

    public JwtAccessTokenProvider(
            JwtEncoder jwtEncoder, JwtConfigurationProperties properties, Clock clock
    ) {
        this.jwtEncoder = jwtEncoder;
        this.properties = properties;
        this.clock = clock;
    }

    @Override
    public GeneratedAccessToken generate(UserAccount userAccount) {
        var issuedAt = clock.instant();
        var expiresAt = issuedAt.plus(properties.accessTokenTtl());

        var headers = JwsHeader.with(MacAlgorithm.HS256).build();
        var claims = JwtClaimsSet.builder()
                .issuer(properties.issuer())
                .subject(userAccount.getId().toString())
                .issuedAt(issuedAt)
                .expiresAt(expiresAt)
                .id(UUID.randomUUID().toString())
                .claim("role", userAccount.getRole().name())
                .claim("password_change_required", userAccount.isPasswordChangeRequired())
                .build();

        var token = jwtEncoder.encode(JwtEncoderParameters.from(headers, claims));
        return new GeneratedAccessToken(token.getTokenValue(), expiresAt);
    }
}
