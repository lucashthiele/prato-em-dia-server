package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountOutput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.in.ProvisionNutritionistAccount;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Locale;

@Service
@Transactional
public class ProvisionNutritionistAccountUseCase implements ProvisionNutritionistAccount {
    private final UserAccountRepository accounts;
    private final PasswordHasher hasher;
    private final InitialPasswordGenerator passwords;
    private final UserProfileInitializer profiles;

    public ProvisionNutritionistAccountUseCase(
            UserAccountRepository accounts, PasswordHasher hasher, InitialPasswordGenerator passwords,
            UserProfileInitializer profiles
    ) {
        this.accounts = accounts;
        this.hasher = hasher;
        this.passwords = passwords;
        this.profiles = profiles;
    }

    @Override
    public ProvisionNutritionistAccountOutput execute(ProvisionNutritionistAccountInput input) {
        var email = input.email().strip().toLowerCase(Locale.ROOT);
        if (accounts.findByEmailIgnoreCase(email).isPresent()) {
            throw new UserAccountAlreadyExistsException();
        }
        var saved = accounts.save(
                new UserAccount(
                        email, hasher.hash(passwords.generate()), UserAccountRole.NUTRITIONIST,
                        UserAccountStatus.ACTIVE, true
                )
        );
        var registration = input.registrationNumber() == null || input.registrationNumber().isBlank()
                ? null
                : input.registrationNumber().strip();
        profiles.initialize(saved.getId(), UserAccountRole.NUTRITIONIST, input.fullName().strip(), registration);
        return new ProvisionNutritionistAccountOutput(saved.getId(), saved.getEmail());
    }
}
