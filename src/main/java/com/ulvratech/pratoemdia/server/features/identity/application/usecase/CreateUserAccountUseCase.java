package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.CreateUserAccount;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import jakarta.transaction.Transactional;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Service
@Transactional
@Slf4j
public class CreateUserAccountUseCase implements CreateUserAccount {
  private final UserAccountRepository repository;
  private final PasswordHasher passwordHasher;
  private final UserProfileInitializer profileInitializer;

  public CreateUserAccountUseCase(
      UserAccountRepository repository,
      PasswordHasher passwordHasher,
      UserProfileInitializer profileInitializer) {
    this.repository = repository;
    this.passwordHasher = passwordHasher;
    this.profileInitializer = profileInitializer;
  }

  @Override
  public CreateUserAccountOutput execute(CreateUserAccountInput input) {
    var lowerCaseUserEmail = input.email().toLowerCase();

    log.info("Creating user account. email={}, role={}", lowerCaseUserEmail, input.role());

    var optionalExistingUser = repository.findByEmailIgnoreCase(lowerCaseUserEmail);

    if (optionalExistingUser.isPresent()) {
      log.error("User with email={} already exists", lowerCaseUserEmail);

      throw new UserAccountAlreadyExistsException();
    }

    String passwordHash = passwordHasher.hash(input.rawPassword());

    log.debug("Password hashed successfully for user. email={}", lowerCaseUserEmail);

    UserAccount userAccount =
        new UserAccount(
            lowerCaseUserEmail,
            passwordHash,
            input.role(),
            UserAccountStatus.ACTIVE); // new user should always be active

    UserAccount savedUserAccount = repository.save(userAccount);
    profileInitializer.initialize(
        savedUserAccount.getId(),
        savedUserAccount.getRole(),
        input.fullName() == null ? null : input.fullName().trim());

    log.info(
        "User account created successfully. userId={}, email={}, role={}, status={}",
        savedUserAccount.getId(),
        savedUserAccount.getEmail(),
        savedUserAccount.getRole(),
        savedUserAccount.getStatus());

    return new CreateUserAccountOutput(
        savedUserAccount.getId(),
        savedUserAccount.getEmail(),
        savedUserAccount.getRole(),
        savedUserAccount.getStatus());
  }
}
