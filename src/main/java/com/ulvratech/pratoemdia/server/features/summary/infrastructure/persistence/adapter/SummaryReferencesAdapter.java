package com.ulvratech.pratoemdia.server.features.summary.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter.PrescriptionQueryAdapter;
import com.ulvratech.pratoemdia.server.features.relationships.infrastructure.persistence.adapter.RelationshipQueryAdapter;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.NutritionistSummaryOutput.DraftToContinue;
import com.ulvratech.pratoemdia.server.features.summary.application.dto.PatientAccessCounts;
import com.ulvratech.pratoemdia.server.features.summary.application.port.out.SummaryReferences;
import java.util.List;
import java.util.UUID;
import org.springframework.stereotype.Component;

@Component
public class SummaryReferencesAdapter implements SummaryReferences {
    private final RelationshipQueryAdapter relationships;
    private final PrescriptionQueryAdapter prescriptions;

    public SummaryReferencesAdapter(
            RelationshipQueryAdapter relationships, PrescriptionQueryAdapter prescriptions
    ) {
        this.relationships = relationships;
        this.prescriptions = prescriptions;
    }

    @Override
    public PatientAccessCounts patientAccessCounts(UUID nutritionistAccountId) {
        var counts = relationships.patientAccessCounts(nutritionistAccountId);
        return new PatientAccessCounts(counts.active(), counts.pending(), counts.failed());
    }

    @Override
    public long countOpenDrafts(UUID nutritionistAccountId) {
        return prescriptions.countOpenDrafts(nutritionistAccountId);
    }

    @Override
    public List<DraftToContinue> openDrafts(UUID nutritionistAccountId, int limit) {
        return prescriptions.openDrafts(nutritionistAccountId, limit).stream()
                .map(
                        d -> new DraftToContinue(
                                d.patientId(), d.patientFullName(), d.prescriptionId(), d.versionId(),
                                d.title(), d.versionNumber(), d.updatedAt()
                        )
                ).toList();
    }
}
