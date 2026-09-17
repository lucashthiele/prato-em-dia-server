package com.ulvratech.pratoemdia.server.features.planning.domain.entity;

import com.ulvratech.pratoemdia.server.features.planning.domain.enums.WeeklyPlanStatus;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import java.time.*;
import java.util.*;

public class WeeklyPlan {
  private final UUID id;
  private final UUID patientId;
  private final UUID prescriptionVersionId;
  private final PlanningMode planningMode;
  private final LocalDate periodStart;
  private final LocalDate periodEnd;
  private final Instant confirmationDeadline;
  private final Instant createdAt;
  private final Instant updatedAt;
  private final long persistenceVersion;
  private WeeklyPlanStatus status;
  private Instant confirmedAt;
  private List<PlannedMeal> meals;

  public WeeklyPlan(
      UUID id,
      UUID patientId,
      UUID prescriptionVersionId,
      PlanningMode planningMode,
      LocalDate periodStart,
      LocalDate periodEnd,
      Instant confirmationDeadline,
      WeeklyPlanStatus status,
      Instant confirmedAt,
      Instant createdAt,
      Instant updatedAt,
      long persistenceVersion,
      List<PlannedMeal> meals) {
    this.id = Objects.requireNonNull(id);
    this.patientId = Objects.requireNonNull(patientId);
    this.prescriptionVersionId = Objects.requireNonNull(prescriptionVersionId);
    this.planningMode = Objects.requireNonNull(planningMode);
    this.periodStart = Objects.requireNonNull(periodStart);
    this.periodEnd = Objects.requireNonNull(periodEnd);
    this.confirmationDeadline = Objects.requireNonNull(confirmationDeadline);
    if (!periodEnd.equals(periodStart.plusDays(6)))
      throw new IllegalArgumentException("A weekly plan must span seven days");
    this.status = Objects.requireNonNull(status);
    this.confirmedAt = confirmedAt;
    if ((status == WeeklyPlanStatus.CONFIRMED) != (confirmedAt != null))
      throw new IllegalArgumentException("Invalid confirmation state");
    this.createdAt = createdAt;
    this.updatedAt = updatedAt;
    this.persistenceVersion = persistenceVersion;
    this.meals = validatedMeals(meals);
  }

  public static WeeklyPlan create(
      UUID patientId,
      UUID versionId,
      PlanningMode mode,
      LocalDate start,
      Instant deadline,
      List<PlannedMeal> meals) {
    return new WeeklyPlan(
        UUID.randomUUID(),
        patientId,
        versionId,
        mode,
        start,
        start.plusDays(6),
        deadline,
        WeeklyPlanStatus.PENDING,
        null,
        null,
        null,
        0,
        meals);
  }

  private static boolean sameStructure(List<PlannedMeal> before, List<PlannedMeal> after) {
    if (!sameSchedule(before, after)) return false;
    var oldItems = before.stream().flatMap(m -> m.items().stream()).toList();
    var newItems = after.stream().flatMap(m -> m.items().stream()).toList();
    if (oldItems.size() != newItems.size()) return false;
    var oldIds =
        oldItems.stream().map(PlannedMealItem::id).collect(java.util.stream.Collectors.toSet());
    return newItems.stream().map(PlannedMealItem::id).allMatch(oldIds::contains);
  }

  private static boolean sameSchedule(List<PlannedMeal> before, List<PlannedMeal> after) {
    if (before.size() != after.size()) return false;
    var expected =
        before.stream()
            .collect(
                java.util.stream.Collectors.toMap(
                    PlannedMeal::id,
                    m ->
                        List.of(
                            m.date(),
                            m.time() == null ? LocalTime.MIDNIGHT : m.time(),
                            m.mealType(),
                            m.name(),
                            m.order())));
    return after.stream()
        .allMatch(
            m ->
                expected.containsKey(m.id())
                    && expected
                        .get(m.id())
                        .equals(
                            List.of(
                                m.date(),
                                m.time() == null ? LocalTime.MIDNIGHT : m.time(),
                                m.mealType(),
                                m.name(),
                                m.order())));
  }

  private static List<PlannedMeal> validatedMeals(List<PlannedMeal> meals) {
    var copy = List.copyOf(meals == null ? List.of() : meals);
    if (copy.stream().map(PlannedMeal::id).distinct().count() != copy.size())
      throw new IllegalArgumentException("Duplicate meal");
    if (copy.stream().map(m -> m.date() + ":" + m.mealType()).distinct().count() != copy.size())
      throw new IllegalArgumentException("Duplicate meal type on the same date");
    if (copy.stream().map(m -> m.date() + ":" + m.order()).distinct().count() != copy.size())
      throw new IllegalArgumentException("Duplicate meal order on the same date");
    return copy;
  }

  public void confirm(Instant now) {
    if (status == WeeklyPlanStatus.CONFIRMED) return;
    status = WeeklyPlanStatus.CONFIRMED;
    confirmedAt = Objects.requireNonNull(now);
  }

  public void replaceMeals(List<PlannedMeal> desired, Instant now) {
    if (!now.isBefore(
        periodEnd.plusDays(1).atStartOfDay(ZoneId.of("America/Sao_Paulo")).toInstant()))
      throw new IllegalStateException("Weekly plan period has ended");
    if (planningMode == PlanningMode.RESTRICTED)
      throw new IllegalStateException("This plan is restricted");
    var replacement = validatedMeals(desired);
    if (replacement.stream()
        .anyMatch(meal -> meal.date().isBefore(periodStart) || meal.date().isAfter(periodEnd)))
      throw new IllegalStateException("Meal date is outside the plan period");
    if (planningMode == PlanningMode.SUBSTITUTIONS_ONLY && !sameStructure(meals, replacement))
      throw new IllegalStateException("Only authorized substitutions are allowed");
    if (status == WeeklyPlanStatus.CONFIRMED && !sameSchedule(meals, replacement))
      throw new IllegalStateException("Confirmed plan schedule cannot be changed");
    meals = replacement;
  }

  public UUID getId() {
    return id;
  }

  public UUID getPatientId() {
    return patientId;
  }

  public UUID getPrescriptionVersionId() {
    return prescriptionVersionId;
  }

  public PlanningMode getPlanningMode() {
    return planningMode;
  }

  public LocalDate getPeriodStart() {
    return periodStart;
  }

  public LocalDate getPeriodEnd() {
    return periodEnd;
  }

  public Instant getConfirmationDeadline() {
    return confirmationDeadline;
  }

  public WeeklyPlanStatus getStatus() {
    return status;
  }

  public Instant getConfirmedAt() {
    return confirmedAt;
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

  public List<PlannedMeal> getMeals() {
    return List.copyOf(meals);
  }
}
