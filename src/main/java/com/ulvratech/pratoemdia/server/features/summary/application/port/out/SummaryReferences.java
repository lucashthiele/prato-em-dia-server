package com.ulvratech.pratoemdia.server.features.summary.application.port.out;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput.DraftToContinue;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.PatientAccessCounts;
import java.util.List;
import java.util.UUID;

public interface SummaryReferences {
    PatientAccessCounts patientAccessCounts(UUID nutritionistAccountId);

    long countOpenDrafts(UUID nutritionistAccountId);

    List<DraftToContinue> openDrafts(UUID nutritionistAccountId, int limit);
}
