package com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.mapper;

import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealItemModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.PlannedMealModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanModel;
import com.ulvratech.pratoemdia.server.features.planning.infrastructure.persistence.model.WeeklyPlanningPreferenceModel;
import java.time.DayOfWeek;
import java.util.ArrayList;
import org.springframework.stereotype.Component;

@Component
public class WeeklyPlanningMapper {
    public WeeklyPlanningPreference toDomain(WeeklyPlanningPreferenceModel model) {
        return new WeeklyPlanningPreference(
                model.getPatientId(), DayOfWeek.of(model.getDayOfWeek()), model.isEnabled()
        );
    }

    public WeeklyPlanningPreferenceModel toModel(WeeklyPlanningPreference value) {
        return WeeklyPlanningPreferenceModel.builder()
                .patientId(value.patientId())
                .dayOfWeek(value.dayOfWeek().getValue())
                .enabled(value.enabled())
                .build();
    }

    public WeeklyPlanModel toModel(WeeklyPlan value) {
        var root = WeeklyPlanModel.builder()
                .id(value.getId())
                .patientId(value.getPatientId())
                .prescriptionVersionId(value.getPrescriptionVersionId())
                .planningMode(value.getPlanningMode())
                .periodStart(value.getPeriodStart())
                .periodEnd(value.getPeriodEnd())
                .confirmationDeadline(value.getConfirmationDeadline())
                .status(value.getStatus())
                .confirmedAt(value.getConfirmedAt())
                .createdAt(value.getCreatedAt())
                .updatedAt(value.getUpdatedAt())
                .version(value.getPersistenceVersion())
                .build();
        root.setMeals(
                value.getMeals().stream()
                        .map(m -> toModel(root, m))
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return root;
    }

    private PlannedMealModel toModel(WeeklyPlanModel plan, PlannedMeal value) {
        var meal = PlannedMealModel.builder()
                .id(value.id())
                .plan(plan)
                .prescribedMealId(value.prescribedMealId())
                .date(value.date())
                .time(value.time())
                .mealType(value.mealType())
                .name(value.name())
                .order(value.order())
                .build();
        meal.setItems(
                value.items().stream()
                        .map(
                                i -> PlannedMealItemModel.builder()
                                        .id(i.id())
                                        .meal(meal)
                                        .prescribedMealItemId(i.prescribedMealItemId())
                                        .allowedSubstitutionId(i.allowedSubstitutionId())
                                        .foodId(i.foodId())
                                        .foodPortionId(i.foodPortionId())
                                        .quantity(i.quantity())
                                        .unit(i.unit())
                                        .order(i.order())
                                        .build()
                        )
                        .collect(java.util.stream.Collectors.toCollection(ArrayList::new))
        );
        return meal;
    }

    public WeeklyPlan toDomain(WeeklyPlanModel model) {
        return new WeeklyPlan(
                model.getId(),
                model.getPatientId(),
                model.getPrescriptionVersionId(),
                model.getPlanningMode(),
                model.getPeriodStart(),
                model.getPeriodEnd(),
                model.getConfirmationDeadline(),
                model.getStatus(),
                model.getConfirmedAt(),
                model.getCreatedAt(),
                model.getUpdatedAt(),
                model.getVersion(),
                model.getMeals().stream()
                        .map(
                                m -> new PlannedMeal(
                                        m.getId(),
                                        m.getPrescribedMealId(),
                                        m.getDate(),
                                        m.getTime(),
                                        m.getMealType(),
                                        m.getName(),
                                        m.getOrder(),
                                        m.getItems().stream()
                                                .map(
                                                        i -> new PlannedMealItem(
                                                                i.getId(),
                                                                i.getPrescribedMealItemId(),
                                                                i.getAllowedSubstitutionId(),
                                                                i.getFoodId(),
                                                                i.getFoodPortionId(),
                                                                i.getQuantity(),
                                                                i.getUnit(),
                                                                i.getOrder()
                                                        )
                                                )
                                                .toList()
                                )
                        )
                        .toList()
        );
    }
}
