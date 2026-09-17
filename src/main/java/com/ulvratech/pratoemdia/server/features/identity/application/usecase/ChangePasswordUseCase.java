package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.AuthenticateUserOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ChangePasswordInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.InvalidCredentialsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ChangePassword;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.AccessTokenProvider;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.RefreshTokenHasher;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.RefreshToken;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.RefreshTokenRepository;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.Duration;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ChangePasswordUseCase implements ChangePassword {
  private final UserAccountRepository accounts;
  private final RefreshTokenRepository refreshTokens;
  private final PasswordHasher passwordHasher;
  private final AccessTokenProvider accessTokens;
  private final RefreshTokenGenerator refreshTokenGenerator;
  private final RefreshTokenHasher refreshTokenHasher;
  private final Clock clock;

  public ChangePasswordUseCase(
      UserAccountRepository accounts,
      RefreshTokenRepository refreshTokens,
      PasswordHasher passwordHasher,
      AccessTokenProvider accessTokens,
      RefreshTokenGenerator refreshTokenGenerator,
      RefreshTokenHasher refreshTokenHasher,
      Clock clock) {
    this.accounts = accounts;
    this.refreshTokens = refreshTokens;
    this.passwordHasher = passwordHasher;
    this.accessTokens = accessTokens;
    this.refreshTokenGenerator = refreshTokenGenerator;
    this.refreshTokenHasher = refreshTokenHasher;
    this.clock = clock;
  }

  @Override
  public AuthenticateUserOutput execute(ChangePasswordInput input) {
    var account =
        accounts.findById(input.userAccountId()).orElseThrow(InvalidCredentialsException::new);
    if (!passwordHasher.matches(input.currentPassword(), account.getPasswordHash())) {
      throw new InvalidCredentialsException();
    }
    account.replacePassword(passwordHasher.hash(input.newPassword()), false);
    accounts.save(account);

    var now = clock.instant();
    refreshTokens.revokeAllByUserAccountId(account.getId(), now);
    var generated = refreshTokenGenerator.generate();
    refreshTokens.save(
        RefreshToken.issue(
            UUID.randomUUID(),
            account.getId(),
            refreshTokenHasher.hash(generated.value()),
            generated.expiresAt(),
            now));
    var access = accessTokens.generate(account);
    return new AuthenticateUserOutput(
        access.value(),
        generated.value(),
        "Bearer",
        Duration.between(now, access.expiresAt()).toSeconds(),
        false);
  }
}
