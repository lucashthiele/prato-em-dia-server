package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.port.in.CreateUserAccount;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
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

    public CreateUserAccountUseCase(UserAccountRepository repository, PasswordHasher passwordHasher) {
        this.repository = repository;
        this.passwordHasher = passwordHasher;
    }

    @Override
    public CreateUserAccountOutput execute(CreateUserAccountInput input) {
        log.info(
            "Creating user account. email={}, role={}",
            input.email(),
            input.role()
        );
        
        String passwordHash = passwordHasher.hash(input.rawPassword());
        
        log.debug(
            "Password hashed successfully for user. email={}",
            input.email()
        );
        
        UserAccount userAccount = new UserAccount(
            input.email(),
            passwordHash,
            input.role(),
            UserAccountStatus.ACTIVE); // new user should always be active
        
        UserAccount savedUserAccount = repository.save(userAccount);

        log.info(
            "User account created successfully. userId={}, email={}, role={}, status={}",
            savedUserAccount.getId(),
            savedUserAccount.getEmail(),
            savedUserAccount.getRole(),
            savedUserAccount.getStatus()
        );
        
        return new CreateUserAccountOutput(
            savedUserAccount.getId(),
            savedUserAccount.getEmail(),
            savedUserAccount.getRole(),
            savedUserAccount.getStatus());
    }
}
 