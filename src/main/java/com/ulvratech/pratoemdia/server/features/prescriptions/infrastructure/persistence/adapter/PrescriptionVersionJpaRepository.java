package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.DraftSummary;
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

    @Query("""
            select v from PrescriptionVersionModel v
            where v.prescription.patientId = :patientId
              and v.prescription.status = :prescriptionStatus and v.status = :versionStatus
            order by v.publishedAt desc
            """)
    List<PrescriptionVersionModel> findCurrentPublished(
            @Param("patientId") UUID patientId,
            @Param("prescriptionStatus") PrescriptionStatus prescriptionStatus,
            @Param("versionStatus") PrescriptionVersionStatus versionStatus, Pageable pageable
    );

    String OPEN_DRAFTS = """
            from PrescriptionVersionModel v join v.prescription p,
                 NutritionistProfileModel n, PatientProfileModel pt, RelationshipModel r
            where n.userAccountId = :accountId and p.nutritionistId = n.id
              and p.patientId = pt.id and p.status = 'ACTIVE' and v.status = 'DRAFT'
              and r.nutritionistId = n.id and r.patientId = pt.id and r.status = 'ACTIVE'
            """;

    @Query("select new com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity."
            + "DraftSummary(pt.id, pt.fullName, p.id, v.id, v.title, v.number, v.updatedAt) "
            + OPEN_DRAFTS + "order by v.updatedAt desc, v.id")
    List<DraftSummary> findOpenDrafts(@Param("accountId") UUID accountId, Pageable pageable);

    @Query("select count(v) " + OPEN_DRAFTS)
    long countOpenDrafts(@Param("accountId") UUID accountId);

    List<PrescriptionVersionModel> findAllByPrescriptionPatientIdAndPublishedAtIsNotNullOrderByPublishedAtAsc(
            UUID patientId
    );
}
