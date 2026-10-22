package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.model.RelationshipModel;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface RelationshipJpaRepository extends JpaRepository<RelationshipModel, UUID> {
    String VIEW = "select new com.ulvratech.pratoemdia.server.features.relationships.domain.entity."
            + "RelationshipView(r.id,p.id,p.userAccountId,p.fullName,pa.email,"
            + "n.id,n.userAccountId,n.fullName,r.status,r.emailDeliveryStatus,r.createdAt) from"
            + " RelationshipModel r, PatientProfileModel p, NutritionistProfileModel n,"
            + " UserAccountModel pa where r.patientId=p.id and r.nutritionistId=n.id and"
            + " p.userAccountId=pa.id ";

    String FILTERED = "and n.userAccountId=:accountId and r.status='ACTIVE'"
            + " and (lower(function('unaccent', p.fullName)) like :pattern escape '\\'"
            + " or lower(pa.email) like :pattern escape '\\')"
            + " and (:filterStatus = false or r.emailDeliveryStatus = :status)"
            + " and (:openDraftOnly = false or exists (select 1 from PrescriptionVersionModel v"
            + " where v.prescription.nutritionistId = r.nutritionistId"
            + " and v.prescription.patientId = r.patientId and v.status = 'DRAFT'))";

    Optional<RelationshipModel> findByNutritionistIdAndPatientIdAndStatus(
            UUID nutritionistId, UUID patientId, RelationshipStatus status
    );

    Optional<RelationshipModel> findByPatientIdAndStatus(UUID patientId, RelationshipStatus status);

    @Query(VIEW + "and n.userAccountId=:accountId and p.id=:patientId and r.status='ACTIVE'")
    Optional<RelationshipView> findActiveViewByNutritionist(
            @Param("accountId") UUID accountId, @Param("patientId") UUID patientId
    );

    @Query(VIEW + "and p.userAccountId=:accountId and r.status='ACTIVE'")
    Optional<RelationshipView> findActiveViewByPatientAccount(@Param("accountId") UUID accountId);

    @Query(VIEW + "and r.id=:relationshipId")
    Optional<RelationshipView> findViewById(@Param("relationshipId") UUID relationshipId);

    @Query(value = VIEW + FILTERED, countQuery = "select count(r) from RelationshipModel r,"
            + " PatientProfileModel p, NutritionistProfileModel n, UserAccountModel pa where"
            + " r.patientId=p.id and r.nutritionistId=n.id and p.userAccountId=pa.id "
            + FILTERED)
    Page<RelationshipView> findActiveByNutritionistAccount(
            @Param("accountId") UUID accountId,
            @Param("pattern") String pattern,
            @Param("filterStatus") boolean filterStatus,
            @Param("status") RelationshipEmailStatus status,
            @Param("openDraftOnly") boolean openDraftOnly,
            Pageable pageable
    );

    @Query("select n.id from NutritionistProfileModel n where n.userAccountId=:accountId")
    Optional<UUID> findNutritionistProfileIdByAccount(@Param("accountId") UUID accountId);

    @Query("select p.id from PatientProfileModel p where p.userAccountId=:accountId")
    Optional<UUID> findPatientProfileIdByAccount(@Param("accountId") UUID accountId);
}
