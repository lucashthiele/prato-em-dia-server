package com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.relationships.domain.entity.RelationshipView;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.model.RelationshipModel;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RelationshipJpaRepository extends JpaRepository<RelationshipModel, UUID> {
  String VIEW =
      "select new com.ulvratech.pratoemdia.server.features.relationships.domain.entity."
          + "RelationshipView(r.id,p.id,p.userAccountId,p.fullName,pa.email,"
          + "n.id,n.userAccountId,n.fullName,r.status,r.emailDeliveryStatus,r.createdAt) from"
          + " RelationshipModel r, PatientProfileModel p, NutritionistProfileModel n,"
          + " UserAccountModel pa where r.patientId=p.id and r.nutritionistId=n.id and"
          + " p.userAccountId=pa.id ";

  Optional<RelationshipModel> findByNutritionistIdAndPatientIdAndStatus(
      UUID nutritionistId, UUID patientId, RelationshipStatus status);

  Optional<RelationshipModel> findByPatientIdAndStatus(UUID patientId, RelationshipStatus status);

  @Query(VIEW + "and n.userAccountId=:accountId and p.id=:patientId and r.status='ACTIVE'")
  Optional<RelationshipView> findActiveViewByNutritionist(
      @Param("accountId") UUID accountId, @Param("patientId") UUID patientId);

  @Query(VIEW + "and p.userAccountId=:accountId and r.status='ACTIVE'")
  Optional<RelationshipView> findActiveViewByPatientAccount(@Param("accountId") UUID accountId);

  @Query(VIEW + "and r.id=:relationshipId")
  Optional<RelationshipView> findViewById(@Param("relationshipId") UUID relationshipId);

  @Query(
      value = VIEW + "and n.userAccountId=:accountId and r.status='ACTIVE'",
      countQuery =
          "select count(r) from RelationshipModel r, NutritionistProfileModel n where"
              + " r.nutritionistId=n.id and n.userAccountId=:accountId and"
              + " r.status='ACTIVE'")
  Page<RelationshipView> findActiveByNutritionistAccount(
      @Param("accountId") UUID accountId, Pageable pageable);

  @Query("select n.id from NutritionistProfileModel n where n.userAccountId=:accountId")
  Optional<UUID> findNutritionistProfileIdByAccount(@Param("accountId") UUID accountId);

  @Query("select p.id from PatientProfileModel p where p.userAccountId=:accountId")
  Optional<UUID> findPatientProfileIdByAccount(@Param("accountId") UUID accountId);
}
