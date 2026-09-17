package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.profiles.domain.enums.ProfileType;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.NutritionistProfileModel;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.PatientProfileModel;
import org.junit.jupiter.api.Test;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;

class UserProfileMapperTest {
    private final UserProfileMapper mapper = new UserProfileMapper();

    @Test
    void mapsNutritionistInBothDirections() {
        var id = UUID.randomUUID();
        var account = UUID.randomUUID();
        var now = Instant.now();
        var model = NutritionistProfileModel.builder().id(id).userAccountId(account)
                .fullName("Nutritionist").phone("123").registrationNumber("CRN")
                .bio("Bio").createdAt(now).updatedAt(now).version(2).build();

        var domain = mapper.toDomain(model);
        var roundTrip = mapper.toNutritionistModel(domain);

        assertEquals(ProfileType.NUTRITIONIST, domain.getType());
        assertEquals(id, roundTrip.getId());
        assertEquals("CRN", roundTrip.getRegistrationNumber());
        assertEquals("Bio", roundTrip.getBio());
        assertEquals(2, roundTrip.getVersion());
    }

    @Test
    void mapsPatientInBothDirections() {
        var id = UUID.randomUUID();
        var account = UUID.randomUUID();
        var now = Instant.now();
        var birthDate = LocalDate.of(1990, 1, 2);
        var model = PatientProfileModel.builder().id(id).userAccountId(account)
                .fullName("Patient").phone("456").birthDate(birthDate)
                .createdAt(now).updatedAt(now).version(3).build();

        var domain = mapper.toDomain(model);
        var roundTrip = mapper.toPatientModel(domain);

        assertEquals(ProfileType.PATIENT, domain.getType());
        assertEquals(id, roundTrip.getId());
        assertEquals(birthDate, roundTrip.getBirthDate());
        assertEquals("456", roundTrip.getPhone());
        assertEquals(3, roundTrip.getVersion());
    }
}
