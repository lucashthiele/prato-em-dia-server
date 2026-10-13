package com.ulvratech.pratoemdia.server.features.prescriptions.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.AllowedSubstitution;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescribedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.Prescription;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity.PrescriptionVersion;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalTime;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public record PrescriptionOutput(
        UUID id,
        UUID patientId,
        UUID nutritionistId,
        PrescriptionStatus status,
        PlanningMode planningMode,
        Instant createdAt,
        Instant updatedAt,
        List<VersionOutput> versions) {
    public static PrescriptionOutput from(Prescription prescription) {
        return new PrescriptionOutput(
                prescription.getId(),
                prescription.getPatientId(),
                prescription.getNutritionistId(),
                prescription.getStatus(),
                prescription.getPlanningMode(),
                prescription.getCreatedAt(),
                prescription.getUpdatedAt(),
                prescription.getVersions().stream()
                        .sorted(Comparator.comparingInt(PrescriptionVersion::getNumber).reversed())
                        .map(VersionOutput::from)
                        .toList()
        );
    }

    public record VersionOutput(
            UUID id,
            int number,
            PrescriptionVersionStatus status,
            String title,
            String notes,
            UUID createdByUserId,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            List<MealOutput> meals,
            List<NutritionTargetOutput> nutritionTargets) {
        public static VersionOutput from(PrescriptionVersion version) {
            return new VersionOutput(
                    version.getId(),
                    version.getNumber(),
                    version.getStatus(),
                    version.getTitle(),
                    version.getNotes(),
                    version.getCreatedByUserId(),
                    version.getCreatedAt(),
                    version.getPublishedAt(),
                    version.getArchivedAt(),
                    version.getMeals().stream()
                            .sorted(Comparator.comparingInt(PrescribedMeal::order))
                            .map(MealOutput::from)
                            .toList(),
                    version.getNutritionTargets().stream()
                            .map(
                                    target -> new NutritionTargetOutput(
                                            target.nutrientCode(),
                                            target.amount(), target.unit()
                                    )
                            ).toList()
            );
        }
    }

    public record NutritionTargetOutput(String nutrientCode, BigDecimal amount, String unit) {
    }

    public record MealOutput(
            UUID id,
            String name,
            MealType mealType,
            int order,
            LocalTime suggestedTime,
            String notes,
            List<ItemOutput> items) {
        static MealOutput from(PrescribedMeal meal) {
            return new MealOutput(
                    meal.id(),
                    meal.name(),
                    meal.mealType(),
                    meal.order(),
                    meal.suggestedTime(),
                    meal.notes(),
                    meal.items().stream()
                            .sorted(Comparator.comparingInt(PrescribedMealItem::order))
                            .map(ItemOutput::from)
                            .toList()
            );
        }
    }

    public record ItemOutput(
            UUID id,
            UUID foodId,
            UUID foodPortionId,
            BigDecimal quantity,
            String unit,
            int order,
            String notes,
            List<SubstitutionOutput> substitutions) {
        static ItemOutput from(PrescribedMealItem item) {
            return new ItemOutput(
                    item.id(),
                    item.foodId(),
                    item.foodPortionId(),
                    item.quantity(),
                    item.unit(),
                    item.order(),
                    item.notes(),
                    item.substitutions().stream().map(SubstitutionOutput::from).toList()
            );
        }
    }

    public record SubstitutionOutput(
            UUID id, UUID foodId, UUID foodPortionId, BigDecimal quantity, String unit, String notes) {
        static SubstitutionOutput from(AllowedSubstitution substitution) {
            return new SubstitutionOutput(
                    substitution.id(),
                    substitution.foodId(),
                    substitution.foodPortionId(),
                    substitution.quantity(),
                    substitution.unit(),
                    substitution.notes()
            );
        }
    }
}
