package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.ArchivePrescriptionsForRelationship;
import com.ulvratech.pratoemdia.server.features.relationships.application.port.out.RelationshipEndedHandler;
import java.util.UUID;
import org.springframework.stereotype.Component;

/** Ends of a relationship archive the pair's prescription (S-07), in the caller's transaction. */
@Component
public class RelationshipEndedPrescriptionAdapter implements RelationshipEndedHandler {
    private final ArchivePrescriptionsForRelationship archive;

    public RelationshipEndedPrescriptionAdapter(ArchivePrescriptionsForRelationship archive) {
        this.archive = archive;
    }

    @Override
    public void onRelationshipEnded(UUID nutritionistId, UUID patientId) {
        archive.execute(nutritionistId, patientId);
    }
}
