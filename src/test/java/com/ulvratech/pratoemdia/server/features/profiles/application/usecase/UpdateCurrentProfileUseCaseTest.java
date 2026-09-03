package com.ulvratech.pratoemdia.server.features.profiles.application.usecase;

import com.ulvratech.pratoemdia.server.features.profiles.application.dto.UpdateProfileInput;
import com.ulvratech.pratoemdia.server.features.profiles.application.exception.InvalidProfileFieldsException;
import com.ulvratech.pratoemdia.server.features.profiles.domain.entity.UserProfile;
import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.domain.port.out.UserProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class UpdateCurrentProfileUseCaseTest {
    private UserProfileRepository repository;
    private UpdateCurrentProfileUseCase useCase;

    @BeforeEach
    void setUp() {
        repository = mock(UserProfileRepository.class);
        useCase = new UpdateCurrentProfileUseCase(repository);
    }

    @Test
    void replacesPatientFieldsAndClearsOmittedOptionalValues() {
        UUID accountId = UUID.randomUUID();
        var profile = UserProfile.create(accountId, ProfileType.PATIENT, "Old Name");
        profile.replaceCommon("Old Name", "+55 11 99999-9999");
        when(repository.findByUserAccountId(accountId, ProfileType.PATIENT))
            .thenReturn(Optional.of(profile));
        when(repository.save(profile)).thenReturn(profile);

        var output = useCase.execute(input(
            accountId, ProfileType.PATIENT, "New Name",
            null, null, null, LocalDate.of(1990, 1, 2)
        ));

        assertEquals("New Name", output.fullName());
        assertNull(output.phone());
        assertEquals(LocalDate.of(1990, 1, 2), output.birthDate());
    }

    @Test
    void rejectsProfessionalFieldsForPatient() {
        UUID accountId = UUID.randomUUID();

        assertThrows(InvalidProfileFieldsException.class, () -> useCase.execute(input(
            accountId, ProfileType.PATIENT, "Patient Name",
            null, "CRN-123", null, null
        )));

        verify(repository, never()).findByUserAccountId(any(), any());
    }

    private UpdateProfileInput input(
        UUID accountId,
        ProfileType type,
        String fullName,
        String phone,
        String registrationNumber,
        String bio,
        LocalDate birthDate
    ) {
        return new UpdateProfileInput(
            accountId, type, fullName, phone, registrationNumber, bio, birthDate
        );
    }
}
