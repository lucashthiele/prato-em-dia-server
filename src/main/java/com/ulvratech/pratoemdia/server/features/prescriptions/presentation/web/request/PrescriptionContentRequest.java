package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalTime;
import java.util.List;
import java.util.UUID;

public record PrescriptionContentRequest(
        @Size(max = 180) String title, String notes, @NotNull List<@Valid MealRequest> meals,
        List<@Valid TargetRequest> nutritionTargets) {
    public PrescriptionContentRequest(String title, String notes, List<MealRequest> meals) {
        this(title, notes, meals, List.of());
    }

    public PrescriptionContentInput toInput() {
        return new PrescriptionContentInput(
                title, notes, meals.stream().map(MealRequest::toInput).toList(),
                (nutritionTargets == null ? List.<TargetRequest>of() : nutritionTargets).stream()
                        .map(
                                target -> new PrescriptionContentInput.TargetInput(
                                        target.nutrientCode(), target.amount(), target.unit()
                                )
                        ).toList()
        );
    }

    public record TargetRequest(@NotBlank @Size(max = 64) String nutrientCode,
            @NotNull @DecimalMin("0") BigDecimal amount,
            @NotBlank @Size(max = 16) String unit) {
    }

    public record MealRequest(
            @NotBlank @Size(max = 120) String name,
            @NotNull MealType mealType,
            @PositiveOrZero int order,
            LocalTime suggestedTime,
            String notes,
            @NotNull List<@Valid ItemRequest> items) {
        public MealRequest(
                String name, int order, LocalTime suggestedTime, String notes, List<ItemRequest> items
        ) {
            this(name, MealType.SNACK, order, suggestedTime, notes, items);
        }

        PrescriptionContentInput.MealInput toInput() {
            return new PrescriptionContentInput.MealInput(
                    name,
                    mealType,
                    order,
                    suggestedTime,
                    notes,
                    items.stream().map(ItemRequest::toInput).toList()
            );
        }
    }

    public record ItemRequest(
            @NotNull UUID foodId,
            UUID foodPortionId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 32) String unit,
            @PositiveOrZero int order,
            String notes,
            @NotNull List<@Valid SubstitutionRequest> substitutions) {
        PrescriptionContentInput.ItemInput toInput() {
            return new PrescriptionContentInput.ItemInput(
                    foodId,
                    foodPortionId,
                    quantity,
                    unit,
                    order,
                    notes,
                    substitutions.stream().map(SubstitutionRequest::toInput).toList()
            );
        }
    }

    public record SubstitutionRequest(
            @NotNull UUID foodId,
            UUID foodPortionId,
            @NotNull @DecimalMin(value = "0.001") BigDecimal quantity,
            @NotBlank @Size(max = 32) String unit,
            String notes) {
        PrescriptionContentInput.SubstitutionInput toInput() {
            return new PrescriptionContentInput.SubstitutionInput(
                    foodId, foodPortionId, quantity, unit, notes
            );
        }
    }
}
