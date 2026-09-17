package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import java.time.Instant;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

public class Prescription {
  private final UUID id;
  private final UUID patientId;
  private final UUID nutritionistId;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final long persistenceVersion;
  private final List<PrescriptionVersion> versions;
  private PrescriptionStatus status;
  private PlanningMode planningMode;

  public Prescription(
      UUID id,
      UUID patientId,
      UUID nutritionistId,
      PrescriptionStatus status,
      Instant createdAt,
      Instant updatedAt,
      long persistenceVersion,
      List<PrescriptionVersion> versions) {
    this(
        id,
        patientId,
        nutritionistId,
        status,
        PlanningMode.RESTRICTED,
        createdAt,
        updatedAt,
        persistenceVersion,
        versions);
  }

  public Prescription(
      UUID id,
      UUID patientId,
      UUID nutritionistId,
      PrescriptionStatus status,
      PlanningMode planningMode,
      Instant createdAt,
      Instant updatedAt,
      long persistenceVersion,
      List<PrescriptionVersion> versions) {
    if (id == null || patientId == null || nutritionistId == null)
      throw new IllegalArgumentException("Prescription identifiers are required");
    this.id = id;
    this.patientId = patientId;
    this.nutritionistId = nutritionistId;
    this.status = status;
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.persistenceVersion = persistenceVersion;
    this.versions = new java.util.ArrayList<>(versions);
    this.planningMode = java.util.Objects.requireNonNull(planningMode, "Planning mode is required");
    validateVersionNumbers();
  }

  public static Prescription create(
      UUID patientId,
      UUID nutritionistId,
      UUID actor,
      String title,
      String notes,
      List<PrescribedMeal> meals) {
    return create(patientId, nutritionistId, actor, PlanningMode.RESTRICTED, title, notes, meals);
  }

  public static Prescription create(
      UUID patientId,
      UUID nutritionistId,
      UUID actor,
      PlanningMode planningMode,
      String title,
      String notes,
      List<PrescribedMeal> meals) {
    return new Prescription(
        UUID.randomUUID(),
        patientId,
        nutritionistId,
        PrescriptionStatus.ACTIVE,
        planningMode,
        null,
        null,
        0,
        List.of(PrescriptionVersion.draft(1, actor, title, notes, meals)));
  }

  public PrescriptionVersion addDraft(UUID actor) {
    if (status != PrescriptionStatus.ACTIVE)
      throw new IllegalStateException("Prescription is archived");
    if (versions.stream().anyMatch(v -> v.getStatus() == PrescriptionVersionStatus.DRAFT))
      throw new IllegalStateException("Prescription already has a draft");
    int next = versions.stream().mapToInt(PrescriptionVersion::getNumber).max().orElse(0) + 1;
    var source =
        versions.stream()
            .max(Comparator.comparingInt(PrescriptionVersion::getNumber))
            .orElseThrow();
    var draft = source.copyAsDraft(next, actor);
    versions.add(draft);
    return draft;
  }

  public PrescriptionVersion requireVersion(UUID versionId) {
    return versions.stream()
        .filter(v -> v.getId().equals(versionId))
        .findFirst()
        .orElseThrow(() -> new IllegalArgumentException("Version does not belong to prescription"));
  }

  public void publish(UUID versionId, Instant now) {
    var target = requireVersion(versionId);
    versions.stream()
        .filter(v -> v.getStatus() == PrescriptionVersionStatus.PUBLISHED)
        .forEach(v -> v.archive(now));
    target.publish(now);
  }

  private void validateVersionNumbers() {
    if (versions.stream().map(PrescriptionVersion::getNumber).distinct().count() != versions.size())
      throw new IllegalArgumentException("Duplicate prescription version number");
  }

  public UUID getId() {
    return id;
  }

  public UUID getPatientId() {
    return patientId;
  }

  public UUID getNutritionistId() {
    return nutritionistId;
  }

  public PrescriptionStatus getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public long getPersistenceVersion() {
    return persistenceVersion;
  }

  public List<PrescriptionVersion> getVersions() {
    return List.copyOf(versions);
  }

  public PlanningMode getPlanningMode() {
    return planningMode;
  }

  public void changePlanningMode(PlanningMode planningMode) {
    this.planningMode = java.util.Objects.requireNonNull(planningMode, "Planning mode is required");
  }
}
