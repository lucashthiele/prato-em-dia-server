package com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.profiles.infrastructure.persistence.model.PatientProfileModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

interface PatientProfileJpaRepository extends JpaRepository<PatientProfileModel, UUID> {
  Optional<PatientProfileModel> findByUserAccountId(UUID userAccountId);
}
