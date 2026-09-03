package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionPatientAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class ProvisionPatientAccountUseCaseTest {
    @Test
    void createsTemporaryPatientCredentialsAndProfile() {
        var accounts = mock(UserAccountRepository.class);
        var hasher = mock(PasswordHasher.class);
        var passwords = mock(InitialPasswordGenerator.class);
        var profiles = mock(UserProfileInitializer.class);
        var id = UUID.randomUUID();
        when(accounts.findByEmailIgnoreCase("patient@example.com")).thenReturn(Optional.empty());
        when(passwords.generate()).thenReturn("TemporaryPassword123");
        when(hasher.hash("TemporaryPassword123")).thenReturn("hash");
        when(accounts.save(any()))
            .thenReturn(
                new UserAccount(
                    id,
                    "patient@example.com",
                    "hash",
                    UserAccountRole.PATIENT,
                    UserAccountStatus.ACTIVE,
                    true));
        var result =
            new ProvisionPatientAccountUseCase(accounts, hasher, passwords, profiles)
                .execute(
                    new ProvisionPatientAccountInput(" Patient@Example.com ", " Ana "));
        assertTrue(result.created());
        assertTrue(result.passwordChangeRequired());
        assertEquals("TemporaryPassword123", result.initialPassword());
        verify(profiles).initialize(id, UserAccountRole.PATIENT, "Ana");
    }

    @Test
    void reusesExistingPatientWithoutChangingCredentials() {
        var accounts = mock(UserAccountRepository.class);
        var hasher = mock(PasswordHasher.class);
        var passwords = mock(InitialPasswordGenerator.class);
        var profiles = mock(UserProfileInitializer.class);
        var account =
            new UserAccount(
                UUID.randomUUID(),
                "patient@example.com",
                "old-hash",
                UserAccountRole.PATIENT,
                UserAccountStatus.ACTIVE,
                false);
        when(accounts.findByEmailIgnoreCase("patient@example.com"))
            .thenReturn(Optional.of(account));
        var result =
            new ProvisionPatientAccountUseCase(accounts, hasher, passwords, profiles)
                .execute(
                    new ProvisionPatientAccountInput("patient@example.com", "Ignored"));
        assertFalse(result.created());
        assertNull(result.initialPassword());
        verify(accounts, never()).save(any());
        verifyNoInteractions(passwords, profiles);
    }
}
