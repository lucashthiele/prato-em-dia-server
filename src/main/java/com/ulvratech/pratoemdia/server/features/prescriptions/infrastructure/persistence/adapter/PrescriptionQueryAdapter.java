package com.ulvratech.pratoemdia.server.features.prescriptions.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/** Read-only view of prescription data for other features, backed by this feature's repositories. */
@Component
@Transactional(readOnly = true)
public class PrescriptionQueryAdapter {
    public record PublishedPeriod(UUID prescriptionId, UUID versionId, Instant publishedAt,
            Instant archivedAt) {
    }

    public record NutrientTarget(String nutrientCode, BigDecimal amount, String unit) {
    }

    private final PrescriptionVersionJpaRepository versions;
    private final NutritionTargetJpaRepository targets;
    private final AllowedSubstitutionJpaRepository substitutions;

    PrescriptionQueryAdapter(
            PrescriptionVersionJpaRepository versions, NutritionTargetJpaRepository targets,
            AllowedSubstitutionJpaRepository substitutions
    ) {
        this.versions = versions;
        this.targets = targets;
        this.substitutions = substitutions;
    }

    public Optional<UUID> publishedFreeVersionId(UUID patientId) {
        return versions.findPublishedVersionIds(
                patientId, PrescriptionStatus.ACTIVE, PlanningMode.FREE,
                PrescriptionVersionStatus.PUBLISHED, PageRequest.of(0, 1)
        ).stream().findFirst();
    }

    public List<PublishedPeriod> publishedPeriods(UUID patientId) {
        return versions.findAllByPrescriptionPatientIdAndPublishedAtIsNotNullOrderByPublishedAtAsc(patientId)
                .stream()
                .map(
                        version -> new PublishedPeriod(
                                version.getPrescription().getId(), version.getId(),
                                version.getPublishedAt(), version.getArchivedAt()
                        )
                ).toList();
    }

    public List<NutrientTarget> nutritionTargets(UUID versionId) {
        return targets.findAllByVersionId(versionId).stream()
                .map(target -> new NutrientTarget(target.getNutrientCode(), target.getAmount(), target.getUnit()))
                .toList();
    }

    public boolean substitutionAuthorized(
            UUID substitutionId, UUID prescribedItemId, UUID foodId, UUID portionId
    ) {
        return substitutions.countAuthorized(substitutionId, prescribedItemId, foodId, portionId) > 0;
    }
}
