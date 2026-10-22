package com.ulvratech.pratoemdia.server.features.summary.application.port.in;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.GetSummaryInput;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput;
import com.ulvratech.pratoemdia.server.shared.application.usecase.UseCase;

public interface GetNutritionistSummary extends UseCase<GetSummaryInput, NutritionistSummaryOutput> {
}
