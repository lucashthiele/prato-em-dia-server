package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.LogoutInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidRefreshTokenException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.LogoutUser;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.Clock;

@Service
@Transactional
public class LogoutUserUseCase implements LogoutUser {
    private final RefreshTokenRepository refreshTokenRepository;
    private final RefreshTokenHasher refreshTokenHasher;
    private final Clock clock;

    public LogoutUserUseCase(
        RefreshTokenRepository refreshTokenRepository,
        RefreshTokenHasher refreshTokenHasher,
        Clock clock
    ) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.refreshTokenHasher = refreshTokenHasher;
        this.clock = clock;
    }

    @Override
    public void execute(LogoutInput input) {
        var refreshToken = refreshTokenRepository
            .findByTokenHashForUpdate(refreshTokenHasher.hash(input.refreshToken()))
            .orElseThrow(InvalidRefreshTokenException::new);

        if (refreshToken.isRevoked()
            || !refreshToken.getUserAccountId().equals(input.userAccountId())) {
            throw new InvalidRefreshTokenException();
        }

        refreshToken.revoke(clock.instant(), null);
        refreshTokenRepository.save(refreshToken);
    }
}
