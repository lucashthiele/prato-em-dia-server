package com.ulvratech.pratoemdia.server.features.planning.application.dto;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class PlanningInputs {
    private PlanningInputs() {
    }

    public record Actor(UUID userAccountId, String role) {
    }

    public record PreferenceUpdate(Actor actor, DayOfWeek dayOfWeek, boolean enabled) {
    }

    public record PlanQuery(Actor actor, UUID planId) {
    }

    public record PlanUpdate(Actor actor, UUID planId, long version, List<MealInput> meals) {
        public PlanUpdate {
            meals = List.copyOf(meals == null ? List.of() : meals);
        }
    }

    public record MealInput(
            UUID id,
            LocalDate date,
            LocalTime time,
            MealType mealType,
            String name,
            int order,
            List<ItemInput> items) {
        public MealInput {
            items = List.copyOf(items == null ? List.of() : items);
        }
    }

    public record ItemInput(
            UUID id, UUID foodId, UUID foodPortionId, BigDecimal quantity, String unit, int order) {
    }
}
