package com.ulvratech.pratoemdia.server.features.prescriptions.domain.entity;

import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PrescriptionVersionStatus;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

public class PrescriptionVersion {
    private final UUID id;
    private final int number;
    private final UUID createdByUserId;
    private final Instant createdAt;
    private Instant updatedAt;
    private PrescriptionVersionStatus status;
    private String title;
    private String notes;
    private Instant publishedAt;
    private Instant archivedAt;
    private List<PrescribedMeal> meals;
    private List<NutritionTarget> nutritionTargets;

    public PrescriptionVersion(
            UUID id,
            int number,
            PrescriptionVersionStatus status,
            String title,
            String notes,
            UUID createdByUserId,
            Instant createdAt,
            Instant publishedAt,
            Instant archivedAt,
            List<PrescribedMeal> meals
    ) {
        this(
                id, number, status, title, notes, createdByUserId, createdAt,
                publishedAt, archivedAt, meals, List.of()
        );
    }

    public PrescriptionVersion(
            UUID id, int number, PrescriptionVersionStatus status, String title, String notes,
            UUID createdByUserId, Instant createdAt, Instant publishedAt, Instant archivedAt,
            List<PrescribedMeal> meals, List<NutritionTarget> nutritionTargets
    ) {
        this(
                id, number, status, title, notes, createdByUserId, createdAt, createdAt,
                publishedAt, archivedAt, meals, nutritionTargets
        );
    }

    public PrescriptionVersion(
            UUID id, int number, PrescriptionVersionStatus status, String title, String notes,
            UUID createdByUserId, Instant createdAt, Instant updatedAt, Instant publishedAt,
            Instant archivedAt, List<PrescribedMeal> meals, List<NutritionTarget> nutritionTargets
    ) {
        if (id == null || createdByUserId == null || number < 1)
            throw new IllegalArgumentException("Invalid prescription version");
        this.id = id;
        this.number = number;
        this.status = status;
        this.title = normalizedLimited(title, 180);
        this.notes = AllowedSubstitution.normalized(notes);
        this.createdByUserId = createdByUserId;
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.publishedAt = publishedAt;
        this.archivedAt = archivedAt;
        this.meals = validatedMeals(meals == null ? List.of() : meals);
        this.nutritionTargets = validatedTargets(nutritionTargets);
    }

    public static PrescriptionVersion draft(
            int number, UUID actor, String title, String notes, List<PrescribedMeal> meals
    ) {
        return draft(number, actor, title, notes, meals, List.of());
    }

    public static PrescriptionVersion draft(
            int number, UUID actor, String title, String notes, List<PrescribedMeal> meals,
            List<NutritionTarget> nutritionTargets
    ) {
        return new PrescriptionVersion(
                UUID.randomUUID(),
                number,
                PrescriptionVersionStatus.DRAFT,
                title,
                notes,
                actor,
                null,
                null,
                null,
                meals,
                nutritionTargets
        );
    }

    private static String normalizedLimited(String value, int max) {
        var normalized = AllowedSubstitution.normalized(value);
        if (normalized != null && normalized.length() > max)
            throw new IllegalArgumentException("Title is too long");
        return normalized;
    }

    private static List<PrescribedMeal> validatedMeals(List<PrescribedMeal> meals) {
        var copy = List.copyOf(meals);
        if (copy.stream().map(PrescribedMeal::order).distinct().count() != copy.size())
            throw new IllegalArgumentException("Duplicate meal order");
        if (copy.stream().map(PrescribedMeal::mealType).distinct().count() != copy.size())
            throw new IllegalArgumentException("Duplicate meal type");
        return copy;
    }

    private static List<NutritionTarget> validatedTargets(List<NutritionTarget> targets) {
        var copy = List.copyOf(targets == null ? List.of() : targets);
        if (copy.stream().map(NutritionTarget::nutrientCode).distinct().count() != copy.size())
            throw new IllegalArgumentException("Duplicate nutrition target");
        return copy;
    }

    public PrescriptionVersion copyAsDraft(int nextNumber, UUID actor) {
        return draft(
                nextNumber,
                actor,
                title,
                notes,
                meals.stream()
                        .map(
                                meal -> PrescribedMeal.create(
                                        meal.name(),
                                        meal.mealType(),
                                        meal.order(),
                                        meal.suggestedTime(),
                                        meal.notes(),
                                        meal.items().stream()
                                                .map(
                                                        item -> PrescribedMealItem.create(
                                                                item.foodId(),
                                                                item.foodPortionId(),
                                                                item.quantity(),
                                                                item.unit(),
                                                                item.order(),
                                                                item.notes(),
                                                                item.substitutions().stream()
                                                                        .map(
                                                                                sub -> AllowedSubstitution.create(
                                                                                        sub.foodId(),
                                                                                        sub.foodPortionId(),
                                                                                        sub.quantity(),
                                                                                        sub.unit(),
                                                                                        sub.notes()
                                                                                )
                                                                        )
                                                                        .toList()
                                                        )
                                                )
                                                .toList()
                                )
                        )
                        .toList(),
                nutritionTargets
        );
    }

    public void replaceContent(String title, String notes, List<PrescribedMeal> meals) {
        replaceContent(title, notes, meals, List.of());
    }

    public void replaceContent(
            String title, String notes, List<PrescribedMeal> meals,
            List<NutritionTarget> nutritionTargets
    ) {
        ensureDraft();
        this.title = normalizedLimited(title, 180);
        this.notes = AllowedSubstitution.normalized(notes);
        this.meals = validatedMeals(meals);
        this.nutritionTargets = validatedTargets(nutritionTargets);
        this.updatedAt = Instant.now().truncatedTo(java.time.temporal.ChronoUnit.MICROS);
    }

    public void publish(Instant now) {
        ensureDraft();
        if (meals.isEmpty() || meals.stream().anyMatch(meal -> meal.items().isEmpty()))
            throw new IllegalStateException("A prescription needs meals with items before publication");
        status = PrescriptionVersionStatus.PUBLISHED;
        publishedAt = now;
    }

    public void archive(Instant now) {
        if (status == PrescriptionVersionStatus.ARCHIVED)
            throw new IllegalStateException("Version is already archived");
        status = PrescriptionVersionStatus.ARCHIVED;
        archivedAt = now;
    }

    private void ensureDraft() {
        if (status != PrescriptionVersionStatus.DRAFT)
            throw new IllegalStateException("Only draft versions can be changed");
    }

    public UUID getId() {
        return id;
    }

    public int getNumber() {
        return number;
    }

    public PrescriptionVersionStatus getStatus() {
        return status;
    }

    public String getTitle() {
        return title;
    }

    public String getNotes() {
        return notes;
    }

    public UUID getCreatedByUserId() {
        return createdByUserId;
    }

    public Instant getCreatedAt() {
        return createdAt;
    }

    public Instant getUpdatedAt() {
        return updatedAt;
    }

    public Instant getPublishedAt() {
        return publishedAt;
    }

    public Instant getArchivedAt() {
        return archivedAt;
    }

    public List<PrescribedMeal> getMeals() {
        return meals;
    }

    public List<NutritionTarget> getNutritionTargets() {
        return List.copyOf(nutritionTargets);
    }
}
