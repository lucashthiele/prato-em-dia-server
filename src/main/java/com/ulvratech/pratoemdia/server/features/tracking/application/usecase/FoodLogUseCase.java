package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.CanonicalConversion;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.FoodLog;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.FoodLogTypes.Item;

import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.FoodLogOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
public class FoodLogUseCase implements FoodLogOperations {
    private final TrackingReferences references;

    public FoodLogUseCase(TrackingReferences references) {
        this.references = references;
    }

    @Override
    public FoodLog get(Actor actor, UUID plannedMealId) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new TrackingException(Kind.FORBIDDEN, "Patient access required");
        var patientId = references.patientId(actor.accountId())
                .orElseThrow(() -> new TrackingException(Kind.FORBIDDEN, "Patient access required"));
        var plan = references.planForMeal(plannedMealId)
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Planned meal was not found"));
        if (!plan.getPatientId().equals(patientId))
            throw new TrackingException(Kind.FORBIDDEN, "Planned meal belongs to another patient");
        var meal = plan.getMeals().stream().filter(candidate -> candidate.id().equals(plannedMealId))
                .findFirst().orElseThrow();
        var source = references.prescriptionVersion(plan.getPrescriptionVersionId())
                .orElseThrow(() -> new TrackingException(Kind.INVALID, "Prescription is unavailable"));
        var items = meal.items().stream().map(item -> item(item, source, plan.getPlanningMode()))
                .toList();
        return new FoodLog(plannedMealId, plan.getPlanningMode(), items);
    }

    private Item item(PlannedMealItem planned, PrescriptionPlanningSource source, PlanningMode mode) {
        List<Item> substitutions = mode != PlanningMode.SUBSTITUTIONS_ONLY ? List.of()
                : source.meals().stream().flatMap(meal -> meal.items().stream())
                        .filter(candidate -> candidate.id().equals(planned.prescribedMealItemId()))
                        .flatMap(candidate -> candidate.substitutions().stream())
                        .map(
                                sub -> snapshot(
                                        planned.id(), sub.foodId(), sub.portionId(), sub.unit(),
                                        sub.quantity(), sub.id(), List.of()
                                )
                        )
                        .toList();
        return snapshot(
                planned.id(), planned.foodId(), planned.foodPortionId(), planned.unit(),
                planned.quantity(), null, substitutions
        );
    }

    private Item snapshot(
            UUID plannedItemId, UUID foodId, UUID portionId, String unit,
            BigDecimal quantity, UUID substitutionId, List<Item> substitutions
    ) {
        FoodPortion portion = portionId == null ? null
                : references.activePortion(foodId, portionId).orElse(null);
        BigDecimal factor = portion == null ? null
                : quantity.divide(portion.getQuantity(), 12, RoundingMode.HALF_UP);
        BigDecimal grams = portion != null && portion.getGramsEquivalent() != null
                ? factor.multiply(portion.getGramsEquivalent()).stripTrailingZeros()
                : "g".equalsIgnoreCase(unit) ? quantity : null;
        BigDecimal kcal = portion == null || factor == null
                || portion.getNutrients().get("energy_kcal") == null
                || portion.getNutrients().get("energy_kcal").amount() == null ? null
                        : portion.getNutrients().get("energy_kcal").amount().multiply(factor)
                                .stripTrailingZeros();
        return new Item(
                plannedItemId, foodId, portionId, unit, quantity, BigDecimal.ONE,
                grams == null ? null : new CanonicalConversion(grams), kcal, substitutionId,
                substitutions
        );
    }
}
