package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.CreateUserAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CreateUserAccountUseCaseTest {
    @Test
    void createsAccountAndProfileInTheSameUseCase() {
        var accounts = mock(UserAccountRepository.class);
        var hasher = mock(PasswordHasher.class);
        var profileInitializer = mock(UserProfileInitializer.class);
        var useCase = new CreateUserAccountUseCase(accounts, hasher, profileInitializer);
        UUID accountId = UUID.randomUUID();

        when(accounts.findByEmailIgnoreCase("patient@example.com"))
            .thenReturn(Optional.empty());
        when(hasher.hash("secret")).thenReturn("hash");
        when(accounts.save(any(UserAccount.class))).thenReturn(new UserAccount(
            accountId,
            "patient@example.com",
            "hash",
            UserAccountRole.PATIENT,
            UserAccountStatus.ACTIVE
        ));

        var output = useCase.execute(new CreateUserAccountInput(
            "Patient@Example.com", "secret", UserAccountRole.PATIENT, "  Ana Silva  "
        ));

        assertEquals(accountId, output.id());
        verify(profileInitializer).initialize(
            accountId, UserAccountRole.PATIENT, "Ana Silva"
        );
    }
}
