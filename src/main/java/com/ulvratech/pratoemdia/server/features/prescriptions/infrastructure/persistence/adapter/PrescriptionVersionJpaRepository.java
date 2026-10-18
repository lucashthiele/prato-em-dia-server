package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionVersionModel;
import java.util.List;
import java.util.UUID;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

interface PrescriptionVersionJpaRepository extends JpaRepository<PrescriptionVersionModel, UUID> {
    @Query("""
            select v.id from PrescriptionVersionModel v
            where v.prescription.patientId = :patientId
              and v.prescription.status = :prescriptionStatus
              and v.prescription.planningMode = :mode
              and v.status = :versionStatus
            order by v.publishedAt desc
            """)
    List<UUID> findPublishedVersionIds(
            @Param("patientId") UUID patientId,
            @Param("prescriptionStatus") PrescriptionStatus prescriptionStatus,
            @Param("mode") PlanningMode mode,
            @Param("versionStatus") PrescriptionVersionStatus versionStatus, Pageable pageable
    );

    List<PrescriptionVersionModel> findAllByPrescriptionPatientIdAndPublishedAtIsNotNullOrderByPublishedAtAsc(
            UUID patientId
    );
}
