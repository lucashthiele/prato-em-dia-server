package com.ulvratech.pratoemdia.server.features.summary.application.usecase;

import com.ulvratech.pratoemdia.server.features.summary.application.dto.GetSummaryInput;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput.Counts;
import com.ulvratech.pratoemdia.server.features.summary.application.port.in.GetNutritionistSummary;
import com.ulvratech.pratoemdia.server.features.summary.application.port.out.SummaryReferences;
import org.springframework.stereotype.Service;

@Service
public class GetNutritionistSummaryUseCase implements GetNutritionistSummary {
    private final SummaryReferences references;

    public GetNutritionistSummaryUseCase(SummaryReferences references) {
        this.references = references;
    }

    @Override
    public NutritionistSummaryOutput execute(GetSummaryInput input) {
        var access = references.patientAccessCounts(input.accountId());
        return new NutritionistSummaryOutput(
                new Counts(
                        access.active(), access.pending(), access.failed(),
                        references.countOpenDrafts(input.accountId())
                ),
                references.openDrafts(input.accountId(), input.draftLimit())
        );
    }
}
