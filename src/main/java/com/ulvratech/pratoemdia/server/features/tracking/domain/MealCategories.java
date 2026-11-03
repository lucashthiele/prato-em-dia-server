package com.ulvratech.pratoemdia.server.features.tracking.domain;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

/**
 * Derives, for one day, the state and the registration permission of each of the four fixed
 * categories. The state is never stored: it comes from the active executions and the planned items.
 */
public final class MealCategories {
    public static final String NO_ALLOWED_FOODS = "NO_ALLOWED_FOODS";

    public enum State {
        NOT_LOGGED, PARTIAL, LOGGED
    }

    /** Planned meal of the day; {@code mode} is frozen in the plan that owns it. */
    public record PlannedCategory(UUID plannedMealId, MealType mealType, PlanningMode mode,
            List<UUID> plannedItemIds) {
    }

    /** Active execution of the day; {@code coveredItemIds} are the planned items it registered. */
    public record ExecutionFact(MealType mealType, UUID plannedMealId, Set<UUID> coveredItemIds) {
    }

    /** {@code currentMode} is the mode of the published prescription, or null if there is none. */
    public record DayFacts(PlanningMode currentMode, List<PlannedCategory> planned,
            List<ExecutionFact> executions) {
    }

    public record Status(MealType mealType, State state, boolean canRegister, String blockedReason,
            UUID plannedMealId, List<UUID> coveredItemIds, List<UUID> uncoveredItemIds) {
    }

    private MealCategories() {
    }

    public static List<Status> derive(DayFacts facts) {
        var result = new ArrayList<Status>();
        for (var type : MealType.values())
            result.add(status(type, facts));
        return result;
    }

    private static Status status(MealType type, DayFacts facts) {
        var planned = facts.planned().stream().filter(meal -> meal.mealType() == type).findFirst().orElse(null);
        var executions = facts.executions().stream().filter(execution -> execution.mealType() == type).toList();
        var mode = planned != null ? planned.mode() : facts.currentMode();
        var items = planned == null ? List.<UUID>of() : planned.plannedItemIds();
        Set<UUID> covered = new HashSet<>();
        executions.forEach(execution -> covered.addAll(execution.coveredItemIds()));
        var coveredItems = items.stream().filter(covered::contains).toList();
        var uncoveredItems = items.stream().filter(item -> !covered.contains(item)).toList();

        boolean free = mode == PlanningMode.FREE;
        boolean canRegister = free || (mode != null && !items.isEmpty());
        State state;
        if (executions.isEmpty())
            state = State.NOT_LOGGED;
        else if (free || uncoveredItems.isEmpty())
            state = State.LOGGED;
        else
            state = State.PARTIAL;
        return new Status(
                type, state, canRegister, canRegister ? null : NO_ALLOWED_FOODS,
                planned == null ? null : planned.plannedMealId(), coveredItems, uncoveredItems
        );
    }
}
