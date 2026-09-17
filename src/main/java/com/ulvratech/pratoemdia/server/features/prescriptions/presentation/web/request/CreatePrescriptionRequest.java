package com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.dto.PrescriptionContentInput;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.presentation.web.request.PrescriptionContentRequest.MealRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record CreatePrescriptionRequest(
        @NotNull PlanningMode planningMode,
        @Size(max = 180) String title,
        String notes,
        @NotNull List<@Valid MealRequest> meals) {
    public PrescriptionContentInput toInput() {
        return new PrescriptionContentRequest(title, notes, meals).toInput();
    }
}
