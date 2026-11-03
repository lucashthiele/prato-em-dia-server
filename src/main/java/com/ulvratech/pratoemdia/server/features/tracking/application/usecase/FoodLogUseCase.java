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
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
        var portions = references.activePortions(portionIds(meal.items(), source));
        var items = meal.items().stream()
                .map(item -> item(item, source, plan.getPlanningMode(), portions)).toList();
        return new FoodLog(plannedMealId, plan.getPlanningMode(), items);
    }

    private static Set<UUID> portionIds(List<PlannedMealItem> planned, PrescriptionPlanningSource source) {
        Set<UUID> ids = new HashSet<>();
        for (var item : planned) {
            if (item.foodPortionId() != null)
                ids.add(item.foodPortionId());
            source.meals().stream().flatMap(meal -> meal.items().stream())
                    .filter(candidate -> candidate.id().equals(item.prescribedMealItemId()))
                    .flatMap(candidate -> candidate.substitutions().stream())
                    .filter(sub -> sub.portionId() != null).forEach(sub -> ids.add(sub.portionId()));
        }
        return ids;
    }

    private Item item(
            PlannedMealItem planned, PrescriptionPlanningSource source, PlanningMode mode,
            Map<UUID, FoodPortion> portions
    ) {
        List<Item> substitutions = mode != PlanningMode.SUBSTITUTIONS_ONLY ? List.of()
                : source.meals().stream().flatMap(meal -> meal.items().stream())
                        .filter(candidate -> candidate.id().equals(planned.prescribedMealItemId()))
                        .flatMap(candidate -> candidate.substitutions().stream())
                        .map(
                                sub -> snapshot(
                                        planned.id(), sub.foodId(), sub.portionId(), sub.unit(),
                                        sub.quantity(), sub.id(), List.of(), portions
                                )
                        )
                        .toList();
        return snapshot(
                planned.id(), planned.foodId(), planned.foodPortionId(), planned.unit(),
                planned.quantity(), null, substitutions, portions
        );
    }

    private Item snapshot(
            UUID plannedItemId, UUID foodId, UUID portionId, String unit,
            BigDecimal quantity, UUID substitutionId, List<Item> substitutions,
            Map<UUID, FoodPortion> portions
    ) {
        FoodPortion portion = portionId == null ? null : portions.get(portionId);
        if (portion != null && !portion.getFoodId().equals(foodId))
            portion = null;
        BigDecimal grams = portion == null ? null
                : portion.scaledGrams(quantity).orElse(null);
        if (grams == null && "g".equalsIgnoreCase(unit))
            grams = quantity;
        BigDecimal kcal = portion == null ? null
                : portion.scaledNutrient("energy_kcal", quantity).orElse(null);
        return new Item(
                plannedItemId, foodId, portionId, unit, quantity, BigDecimal.ONE,
                grams == null ? null : new CanonicalConversion(grams), kcal, substitutionId,
                substitutions
        );
    }
}
