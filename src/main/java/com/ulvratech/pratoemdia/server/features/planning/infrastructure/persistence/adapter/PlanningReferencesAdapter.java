package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.ItemSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.SubstitutionSource;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.model.PrescriptionVersionModel;
import jakarta.persistence.EntityManager;
import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Transactional(readOnly = true)
public class PlanningReferencesAdapter implements PlanningReferences {
  private final EntityManager entityManager;

  public PlanningReferencesAdapter(EntityManager entityManager) {
    this.entityManager = entityManager;
  }

  @Override
  public Optional<UUID> patientProfileId(UUID userAccountId) {
    return entityManager
        .createQuery("select p.id from PatientProfileModel p where p.userAccountId=:id", UUID.class)
        .setParameter("id", userAccountId)
        .getResultStream()
        .findFirst();
  }

  @Override
  public Optional<PrescriptionPlanningSource> currentPublishedPrescription(UUID patientId) {
    var found =
        entityManager
            .createQuery(
                "select v from PrescriptionVersionModel v join v.prescription p "
                    + "where p.patientId=:patient and p.status=:active and v.status=:published order by v.publishedAt desc",
                PrescriptionVersionModel.class)
            .setParameter("patient", patientId)
            .setParameter("active", PrescriptionStatus.ACTIVE)
            .setParameter("published", PrescriptionVersionStatus.PUBLISHED)
            .setMaxResults(1)
            .getResultStream()
            .findFirst();
    return found.map(this::source);
  }

  @Override
  public Optional<PrescriptionPlanningSource> prescriptionVersion(UUID versionId) {
    return Optional.ofNullable(entityManager.find(PrescriptionVersionModel.class, versionId))
        .map(this::source);
  }

  private PrescriptionPlanningSource source(PrescriptionVersionModel version) {
    return new PrescriptionPlanningSource(
        version.getId(),
        version.getPrescription().getPlanningMode(),
        version.getMeals().stream()
            .map(
                meal ->
                    new MealSource(
                        meal.getId(),
                        meal.getName(),
                        meal.getMealType(),
                        meal.getOrder(),
                        meal.getSuggestedTime(),
                        meal.getItems().stream()
                            .map(
                                item ->
                                    new ItemSource(
                                        item.getId(),
                                        item.getFoodId(),
                                        item.getFoodPortionId(),
                                        item.getQuantity(),
                                        item.getUnit(),
                                        item.getOrder(),
                                        item.getSubstitutions().stream()
                                            .map(
                                                sub ->
                                                    new SubstitutionSource(
                                                        sub.getId(),
                                                        sub.getFoodId(),
                                                        sub.getFoodPortionId(),
                                                        sub.getQuantity(),
                                                        sub.getUnit()))
                                            .toList()))
                            .toList()))
            .toList());
  }

  @Override
  public boolean isActiveFood(UUID foodId) {
    return entityManager
            .createQuery(
                "select count(f) from FoodModel f where f.id=:id and f.active=true", Long.class)
            .setParameter("id", foodId)
            .getSingleResult()
        > 0;
  }

  @Override
  public boolean portionBelongsToFood(UUID portionId, UUID foodId) {
    return entityManager
            .createQuery(
                "select count(p) from FoodPortionModel p "
                    + "where p.id=:portion and p.foodId=:food and p.active=true",
                Long.class)
            .setParameter("portion", portionId)
            .setParameter("food", foodId)
            .getSingleResult()
        > 0;
  }
}
