package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.RefreshAccessTokenOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.ExpiredRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InactiveUserAccountException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.RefreshAccessToken;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;

import jakarta.transaction.Transactional;

import org.springframework.stereotype.Service;

import java.time.Clock;
import java.time.Duration;
import java.util.UUID;

@Service
@Transactional
public class RefreshAccessTokenUseCase implements RefreshAccessToken {
    private final RefreshTokenRepository refreshTokenRepository;
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final AccessTokenProvider accessTokenProvider;
    private final Clock clock;

    public RefreshAccessTokenUseCase(
            RefreshTokenRepository refreshTokenRepository,
            UserAccountRepository userAccountRepository,
            RefreshTokenHasher refreshTokenHasher,
            RefreshTokenGenerator refreshTokenGenerator,
            AccessTokenProvider accessTokenProvider,
            Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.accessTokenProvider = accessTokenProvider;
        this.clock = clock;
    }

    @Override
    public RefreshAccessTokenOutput execute(RefreshAccessTokenInput input) {
        var now = clock.instant();
        var currentToken = refreshTokenRepository
                .findByTokenHashForUpdate(refreshTokenHasher.hash(input.refreshToken()))
                .orElseThrow(InvalidRefreshTokenException::new);

        if (currentToken.isRevoked()) {
            throw new InvalidRefreshTokenException();
        }
        if (currentToken.isExpiredAt(now)) {
            throw new ExpiredRefreshTokenException();
        }

        var userAccount = userAccountRepository
                .findById(currentToken.getUserAccountId())
                .orElseThrow(InvalidRefreshTokenException::new);
        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            throw new InactiveUserAccountException();
        }

        var generatedRefreshToken = refreshTokenGenerator.generate();
        var replacementToken = RefreshToken.issue(
                UUID.randomUUID(),
                userAccount.getId(),
                refreshTokenHasher.hash(generatedRefreshToken.value()),
                generatedRefreshToken.expiresAt(),
                now
        );

        refreshTokenRepository.save(replacementToken);
        currentToken.revoke(now, replacementToken.getId());
        refreshTokenRepository.save(currentToken);

        var accessToken = accessTokenProvider.generate(userAccount);
        return new RefreshAccessTokenOutput(
                accessToken.value(),
                generatedRefreshToken.value(),
                "Bearer",
                Duration.between(now, accessToken.expiresAt()).toSeconds(),
                userAccount.isPasswordChangeRequired()
        );
    }
}
