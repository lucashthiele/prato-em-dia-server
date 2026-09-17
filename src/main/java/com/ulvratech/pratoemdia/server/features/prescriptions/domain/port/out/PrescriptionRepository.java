package com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface PrescriptionRepository {
  Prescription save(Prescription prescription);

  Optional<Prescription> findById(UUID id);

  Optional<Prescription> findByVersionId(UUID versionId);

  List<Prescription> findByNutritionistAndPatient(UUID nutritionistId, UUID patientId);

  Optional<Prescription> findCurrentByPatient(UUID patientId);
}
