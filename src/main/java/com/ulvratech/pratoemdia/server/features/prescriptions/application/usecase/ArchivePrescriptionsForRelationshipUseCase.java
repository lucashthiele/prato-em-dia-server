package com.ulvratech.pratoemdia.server.features.prescriptions.application.usecase;

import com.ulvratech.pratoemdia.server.features.prescriptions.application.port.in.ArchivePrescriptionsForRelationship;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.port.out.PrescriptionRepository;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.util.UUID;
import org.springframework.stereotype.Service;

@Service
@Transactional
public class ArchivePrescriptionsForRelationshipUseCase implements ArchivePrescriptionsForRelationship {
    private final PrescriptionRepository repository;
    private final Clock clock;

    public ArchivePrescriptionsForRelationshipUseCase(PrescriptionRepository repository, Clock clock) {
        this.repository = repository;
        this.clock = clock;
    }

    @Override
    public void execute(UUID nutritionistId, UUID patientId) {
        var now = clock.instant();
        for (var prescription : repository.findByNutritionistAndPatient(nutritionistId, patientId)) {
            prescription.archive(now);
            repository.save(prescription);
        }
    }
}
