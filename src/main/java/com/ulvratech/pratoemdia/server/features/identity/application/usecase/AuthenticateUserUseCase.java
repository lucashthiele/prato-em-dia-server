package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InactiveUserAccountException;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.AuthenticateUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
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
import java.util.Locale;
import java.util.UUID;

@Service
@Transactional
public class AuthenticateUserUseCase implements AuthenticateUser {
    private final UserAccountRepository userAccountRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordHasher passwordHasher;
    private final AccessTokenProvider accessTokenProvider;
    private final RefreshTokenGenerator refreshTokenGenerator;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    public AuthenticateUserUseCase(
        UserAccountRepository userAccountRepository,
        RefreshTokenRepository refreshTokenRepository,
        PasswordHasher passwordHasher,
        AccessTokenProvider accessTokenProvider,
        RefreshTokenGenerator refreshTokenGenerator,
        RefreshTokenHasher refreshTokenHasher,
        Clock clock
    ) {
        this.userAccountRepository = userAccountRepository;
        this.refreshTokenRepository = refreshTokenRepository;
        this.passwordHasher = passwordHasher;
        this.accessTokenProvider = accessTokenProvider;
        this.refreshTokenGenerator = refreshTokenGenerator;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
    }

    @Override
    public AuthenticateUserOutput execute(AuthenticateUserInput input) {
        var normalizedEmail = input.email().strip().toLowerCase(Locale.ROOT);
        var userAccount = userAccountRepository.findByEmailIgnoreCase(normalizedEmail)
            .orElseThrow(InvalidCredentialsException::new);

        if (!passwordHasher.matches(input.rawPassword(), userAccount.getPasswordHash())) {
            throw new InvalidCredentialsException();
        }
        if (userAccount.getStatus() != UserAccountStatus.ACTIVE) {
            throw new InactiveUserAccountException();
        }

        var now = clock.instant();
        var accessToken = accessTokenProvider.generate(userAccount);
        var generatedRefreshToken = refreshTokenGenerator.generate();
        var refreshToken = RefreshToken.issue(
            UUID.randomUUID(),
            userAccount.getId(),
            refreshTokenHasher.hash(generatedRefreshToken.value()),
            generatedRefreshToken.expiresAt(),
            now
        );
        refreshTokenRepository.save(refreshToken);

        return new AuthenticateUserOutput(
            accessToken.value(),
            generatedRefreshToken.value(),
            "Bearer",
            Duration.between(now, accessToken.expiresAt()).toSeconds()
        );
    }
}
