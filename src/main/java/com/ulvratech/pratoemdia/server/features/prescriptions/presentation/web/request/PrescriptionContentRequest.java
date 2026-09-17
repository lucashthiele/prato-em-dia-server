package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput;
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
        @Size(max = 180) String title,
        String notes,
        @NotNull List<@Valid MealRequest> meals) {
    public PrescriptionContentInput toInput() {
        return new PrescriptionContentInput(title, notes, meals.stream().map(MealRequest::toInput).toList());
    }

    public record MealRequest(
            @NotBlank @Size(max = 120) String name,
            @PositiveOrZero int order,
            LocalTime suggestedTime,
            String notes,
            @NotNull List<@Valid ItemRequest> items) {
        PrescriptionContentInput.MealInput toInput() {
            return new PrescriptionContentInput.MealInput(
                    name, order, suggestedTime, notes,
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
                    foodId, foodPortionId, quantity, unit,
                    order, notes, substitutions.stream().map(SubstitutionRequest::toInput).toList()
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
