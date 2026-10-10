package com.ulvratech.pratoemdia.server.features.relationships.domain.entity;

import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipEmailStatus;
import com.ulvratech.pratoemdia.server.features.relationships.domain.enums.RelationshipStatus;

import java.time.Instant;
import java.util.UUID;

public class ProfessionalPatientRelationship {
    private final UUID id;
    private final UUID nutritionistId;
    private final UUID patientId;
    private final Instant createdAt;
    private final Instant updatedAt;
    private final long version;
    private RelationshipStatus status;
    private Instant endedAt;
    private UUID endedByUserAccountId;
    private RelationshipEmailStatus emailDeliveryStatus;
    private int emailAttemptCount;
    private Instant emailLastAttemptAt;

    public ProfessionalPatientRelationship(
            UUID id,
            UUID nutritionistId,
            UUID patientId,
            RelationshipStatus status,
            Instant endedAt,
            UUID endedByUserAccountId,
            RelationshipEmailStatus emailDeliveryStatus,
            int emailAttemptCount,
            Instant emailLastAttemptAt,
            Instant createdAt,
            Instant updatedAt,
            long version
    ) {
        this.id = id;
        this.nutritionistId = nutritionistId;
        this.patientId = patientId;
        this.status = status;
        this.endedAt = endedAt;
        this.endedByUserAccountId = endedByUserAccountId;
        this.emailDeliveryStatus = emailDeliveryStatus;
        this.emailAttemptCount = emailAttemptCount;
        this.emailLastAttemptAt = emailLastAttemptAt;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.version = version;
    }

    public static ProfessionalPatientRelationship create(UUID nutritionistId, UUID patientId) {
        return new ProfessionalPatientRelationship(
                UUID.randomUUID(),
                nutritionistId,
                patientId,
                RelationshipStatus.ACTIVE,
                null,
                null,
                RelationshipEmailStatus.PENDING,
                0,
                null,
                null,
                null,
                0
        );
    }

    public void end(UUID actor, Instant now) {
        if (status != RelationshipStatus.ACTIVE)
            throw new IllegalStateException("Relationship already ended");
        status = RelationshipStatus.ENDED;
        endedAt = now;
        endedByUserAccountId = actor;
    }

    public int startEmailAttempt(Instant now) {
        emailDeliveryStatus = RelationshipEmailStatus.PENDING;
        emailAttemptCount++;
        emailLastAttemptAt = now;
        return emailAttemptCount;
    }

    public boolean completeEmailAttempt(int attempt, RelationshipEmailStatus result) {
        if (emailAttemptCount != attempt || emailDeliveryStatus != RelationshipEmailStatus.PENDING)
            return false;
        emailDeliveryStatus = result;
        return true;
    }

    public UUID getId() {
        return id;
    }

    public UUID getNutritionistId() {
        return nutritionistId;
    }

    public UUID getPatientId() {
        return patientId;
    }

    public RelationshipStatus getStatus() {
        return status;
    }

    public Instant getEndedAt() {
        return endedAt;
    }

    public UUID getEndedByUserAccountId() {
        return endedByUserAccountId;
    }

    public RelationshipEmailStatus getEmailDeliveryStatus() {
        return emailDeliveryStatus;
    }

    public int getEmailAttemptCount() {
        return emailAttemptCount;
    }

    public Instant getEmailLastAttemptAt() {
        return emailLastAttemptAt;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public long getVersion() {
        return version;
    }
}
