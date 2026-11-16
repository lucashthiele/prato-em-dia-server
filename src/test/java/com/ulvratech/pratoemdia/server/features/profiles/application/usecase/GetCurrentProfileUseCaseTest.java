package com.ulvratech.pratoemdia.server.features.profiles.application.usecase;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.GetProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.ProfileNotFoundException;
import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class GetCurrentProfileUseCaseTest {
    private final UserProfileRepository repository = mock(UserProfileRepository.class);
    private final GetCurrentProfileUseCase useCase = new GetCurrentProfileUseCase(
            repository, id -> "conta@exemplo.com"
    );

    @Test
    void returnsTheProfileWithTheAccountEmail() {
        var accountId = UUID.randomUUID();
        var profile = UserProfile.create(accountId, ProfileType.NUTRITIONIST, "Ana");
        when(repository.findByUserAccountId(accountId, ProfileType.NUTRITIONIST))
                .thenReturn(Optional.of(profile));

        var output = useCase.execute(new GetProfileInput(accountId, ProfileType.NUTRITIONIST));

        assertEquals("Ana", output.fullName());
        assertEquals("NUTRITIONIST", output.role());
        assertEquals("conta@exemplo.com", output.email());
    }

    @Test
    void returnsOnlyTheAccountForAnAdminWithoutLookingForAProfile() {
        var accountId = UUID.randomUUID();

        var output = useCase.execute(new GetProfileInput(accountId, null));

        assertEquals("ADMIN", output.role());
        assertEquals(accountId, output.userAccountId());
        assertEquals("conta@exemplo.com", output.email());
        assertNull(output.id());
        assertNull(output.fullName());
        verify(repository, never()).findByUserAccountId(any(), any());
    }

    @Test
    void failsWhenTheProfileIsMissing() {
        var accountId = UUID.randomUUID();
        when(repository.findByUserAccountId(accountId, ProfileType.PATIENT)).thenReturn(Optional.empty());

        assertThrows(
                ProfileNotFoundException.class,
                () -> useCase.execute(new GetProfileInput(accountId, ProfileType.PATIENT))
        );
    }
}
