package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionModel;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PrescriptionJpaRepository extends JpaRepository<PrescriptionModel, UUID> {
  Optional<PrescriptionModel> findDistinctByVersionsId(UUID versionId);

  List<PrescriptionModel> findAllByNutritionistIdAndPatientIdAndStatusOrderByCreatedAtDesc(
      UUID nutritionistId, UUID patientId, PrescriptionStatus status);

  @Query(
      "select distinct p from PrescriptionModel p join p.versions v "
          + "where p.patientId=:patientId and p.status=:rootStatus and v.status=:versionStatus "
          + "order by v.publishedAt desc")
  List<PrescriptionModel> findCurrentByPatient(
      @Param("patientId") UUID patientId,
      @Param("rootStatus") PrescriptionStatus rootStatus,
      @Param("versionStatus") PrescriptionVersionStatus versionStatus,
      Pageable pageable);
}
