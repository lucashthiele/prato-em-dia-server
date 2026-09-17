package com.ulvratech.pratoemdia.server.features.planning.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.ItemInput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.MealInput;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.DayOfWeek;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public final class WeeklyPlanningRequests {
    private WeeklyPlanningRequests() {
    }

    public record PreferenceRequest(@NotNull DayOfWeek dayOfWeek, boolean enabled) {
    }

    public record UpdatePlanRequest(
            @PositiveOrZero long version, @NotNull List<@Valid PlannedMealRequest> meals) {
    }

    public record PlannedMealRequest(
            UUID id,
            @NotNull LocalDate date,
            LocalTime time,
            @NotNull MealType mealType,
            @NotBlank @Size(max = 120) String name,
            @PositiveOrZero int order,
            @NotNull List<@Valid PlannedItemRequest> items) {
        public MealInput toInput() {
            return new MealInput(
                    id,
                    date,
                    time,
                    mealType,
                    name,
                    order,
                    items.stream().map(PlannedItemRequest::toInput).toList()
            );
        }
    }

    public record PlannedItemRequest(
            UUID id,
            @NotNull UUID foodId,
            UUID foodPortionId,
            @NotNull @DecimalMin("0.001") BigDecimal quantity,
            @NotBlank @Size(max = 32) String unit,
            @PositiveOrZero int order) {
        public ItemInput toInput() {
            return new ItemInput(id, foodId, foodPortionId, quantity, unit, order);
        }
    }
}
