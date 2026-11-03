package com.ulvratech.pratoemdia.server.features.identity.application.usecase;

import com.ulvratech.pratoemdia.server.features.identity.application.dto.ProvisionNutritionistAccountInput;
import com.ulvratech.pratoemdia.server.features.identity.application.exception.UserAccountAlreadyExistsException;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.InitialPasswordGenerator;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.PasswordHasher;
import com.ulvratech.pratoemdia.server.features.identity.application.port.out.UserProfileInitializer;
import com.ulvratech.pratoemdia.server.features.identity.domain.entity.UserAccount;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountRole;
import com.ulvratech.pratoemdia.server.features.identity.domain.enums.UserAccountStatus;
import com.ulvratech.pratoemdia.server.features.identity.domain.port.out.UserAccountRepository;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProvisionNutritionistAccountUseCaseTest {
    private final UserAccountRepository accounts = mock(UserAccountRepository.class);
    private final PasswordHasher hasher = mock(PasswordHasher.class);
    private final InitialPasswordGenerator passwords = mock(InitialPasswordGenerator.class);
    private final UserProfileInitializer profiles = mock(UserProfileInitializer.class);
    private final ProvisionNutritionistAccountUseCase useCase = new ProvisionNutritionistAccountUseCase(
            accounts, hasher, passwords, profiles
    );

    @Test
    void createsAnActiveNutritionistThatMustChangeAnUnknownPasswordAndItsProfile() {
        var id = UUID.randomUUID();
        when(passwords.generate()).thenReturn("throwaway");
        when(hasher.hash("throwaway")).thenReturn("hash");
        when(accounts.findByEmailIgnoreCase("ana@example.com")).thenReturn(Optional.empty());
        when(accounts.save(any())).thenAnswer(
                i -> {
                    UserAccount a = i.getArgument(0);
                    return new UserAccount(
                            id, a.getEmail(), a.getPasswordHash(), a.getRole(), a.getStatus(),
                            a.isPasswordChangeRequired()
                    );
                }
        );

        var output = useCase
                .execute(new ProvisionNutritionistAccountInput(" Ana@Example.com ", " Ana Souza ", "CRN-3 1234"));

        var saved = ArgumentCaptor.forClass(UserAccount.class);
        verify(accounts).save(saved.capture());
        assertEquals("ana@example.com", saved.getValue().getEmail());
        assertEquals(UserAccountRole.NUTRITIONIST, saved.getValue().getRole());
        assertEquals(UserAccountStatus.ACTIVE, saved.getValue().getStatus());
        assertTrue(saved.getValue().isPasswordChangeRequired());
        assertEquals("hash", saved.getValue().getPasswordHash());
        verify(profiles).initialize(id, UserAccountRole.NUTRITIONIST, "Ana Souza", "CRN-3 1234");
        assertEquals(id, output.userAccountId());
        assertEquals("ana@example.com", output.email());
    }

    @Test
    void rejectsAnEmailThatAlreadyHasAnAccount() {
        when(accounts.findByEmailIgnoreCase("ana@example.com")).thenReturn(
                Optional.of(new UserAccount("ana@example.com", "h", UserAccountRole.PATIENT, UserAccountStatus.ACTIVE))
        );

        assertThrows(
                UserAccountAlreadyExistsException.class,
                () -> useCase.execute(new ProvisionNutritionistAccountInput("ana@example.com", "Ana", null))
        );
        verify(accounts, never()).save(any());
    }
}
