package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.PatientProfileModel;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

interface PatientProfileJpaRepository extends JpaRepository<PatientProfileModel, UUID> {
    Optional<PatientProfileModel> findByUserAccountId(UUID userAccountId);
}
