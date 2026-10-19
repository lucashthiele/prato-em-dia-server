package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.NutritionistProfileModel;
import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.PatientProfileModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of profile data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class ProfileQueryAdapter {
    private final PatientProfileJpaRepository patients;
    private final NutritionistProfileJpaRepository nutritionists;

    ProfileQueryAdapter(
            PatientProfileJpaRepository patients, NutritionistProfileJpaRepository nutritionists
    ) {
        this.patients = patients;
        this.nutritionists = nutritionists;
    }

    public Optional<UUID> patientProfileId(UUID accountId) {
        return patients.findByUserAccountId(accountId).map(PatientProfileModel::getId);
    }

    public Optional<UUID> nutritionistProfileId(UUID accountId) {
        return nutritionists.findByUserAccountId(accountId).map(NutritionistProfileModel::getId);
    }

    public Optional<PatientContact> patientContact(UUID patientId) {
        return patients.findById(patientId)
                .map(patient -> new PatientContact(patient.getFullName(), patient.getUserAccountId()));
    }

    public record PatientContact(String fullName, UUID userAccountId) {
    }
}
