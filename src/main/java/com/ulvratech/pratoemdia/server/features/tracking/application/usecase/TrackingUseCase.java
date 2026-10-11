package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.catalog.domain.entity.FoodPortion;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.CanonicalConversion;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Create;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.ConsumedItem;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Event;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.FoodLog;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.FoodLogItem;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.LoggedItem;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Summary;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.in.TrackingOperations;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.TrackingReferences;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import com.ulvratech.pratoemdia.server.features.tracking.domain.port.out.MealCompletionRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.HexFormat;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class TrackingUseCase implements TrackingOperations {
    private static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final MealCompletionRepository repository;
    private final TrackingReferences references;
    private final ObjectMapper json;
    private final Clock clock;

    public TrackingUseCase(
            MealCompletionRepository repository, TrackingReferences references,
            ObjectMapper json, Clock clock
    ) {
        this.repository = repository;
        this.references = references;
        this.json = json;
        this.clock = clock;
    }

    @Override
    public Event create(Create input) {
        UUID patient = requirePatient(input.actor());
        if (input.idempotencyKey() == null || input.idempotencyKey().isBlank()
                || input.idempotencyKey().length() > 255)
            throw invalid("Idempotency-Key is required and must be at most 255 characters");
        if (input.occurredAt() == null)
            throw invalid("occurredAt is required");
        String hash = hash(input);
        var existing = repository.byKey(patient, input.idempotencyKey());
        if (existing.isPresent())
            return sameOrConflict(existing.get(), hash);
        var plan = references.planForMeal(input.plannedMealId())
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Planned meal was not found"));
        if (!plan.getPatientId().equals(patient))
            throw denied();
        var meal = plan.getMeals().stream().filter(m -> m.id().equals(input.plannedMealId()))
                .findFirst().orElseThrow();
        Instant now = clock.instant();
        if (input.occurredAt().isAfter(now))
            throw invalid("occurredAt cannot be in the future");
        var date = input.occurredAt().atZone(ZONE).toLocalDate();
        if (date.isBefore(plan.getPeriodStart()) || date.isAfter(plan.getPeriodEnd()))
            throw invalid("occurredAt is outside the plan period");
        var source = references.prescriptionVersion(plan.getPrescriptionVersionId())
                .orElseThrow(() -> invalid("Prescription version is unavailable"));
        List<LoggedItem> consumed = resolveConsumed(plan.getPlanningMode(), meal, source, input.consumedItems());
        Type eventType = consumed.isEmpty() ? Type.NOT_COMPLETED
                : consumed.stream().anyMatch(i -> i.allowedSubstitutionId() != null)
                        ? Type.SUBSTITUTED : Type.COMPLETED;
        var candidate = new MealCompletionEvent(
                0, UUID.randomUUID(), patient, plan.getId(),
                meal.id(), eventType, input.reason(), input.occurredAt(), now,
                input.idempotencyKey(), hash, encode(meal), encode(plannedSnapshot(meal)), encode(consumed)
        );
        var saved = repository.append(candidate);
        return sameOrConflict(saved, hash);
    }

    @Override
    @Transactional(readOnly = true)
    public FoodLog foodLog(Actor actor, UUID plannedMealId) {
        UUID patient = requirePatient(actor);
        var plan = references.planForMeal(plannedMealId)
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Planned meal was not found"));
        if (!plan.getPatientId().equals(patient))
            throw denied();
        var meal = plan.getMeals().stream().filter(m -> m.id().equals(plannedMealId))
                .findFirst().orElseThrow();
        var source = references.prescriptionVersion(plan.getPrescriptionVersionId())
                .orElseThrow(() -> invalid("Prescription version is unavailable"));
        return new FoodLog(meal.id(), plan.getPlanningMode(), meal.items().stream()
                .map(item -> foodLogItem(item, source, plan.getPlanningMode())).toList());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Event> mealEvents(Actor actor, UUID mealId, int page, int size) {
        UUID patient = requirePatient(actor);
        var plan = references.planForMeal(mealId).orElse(null);
        if (plan != null && !plan.getPatientId().equals(patient))
            throw denied();
        if (plan == null) {
            UUID eventPatient = repository.patientForMeal(mealId)
                    .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Planned meal was not found"));
            if (!eventPatient.equals(patient))
                throw denied();
        }
        var result = repository.forMeal(mealId, checkedPage(page), checkedSize(size));
        return map(result);
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<Event> patientEvents(Actor actor, int page, int size) {
        return map(repository.forPatient(requirePatient(actor), checkedPage(page), checkedSize(size)));
    }

    @Override
    @Transactional(readOnly = true)
    public Summary summary(Actor actor, UUID patientId, UUID planId) {
        if (actor == null || !"NUTRITIONIST".equals(actor.role())
                || !references.hasActiveRelationship(actor.accountId(), patientId))
            throw denied();
        var plan = references.plan(planId)
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Weekly plan was not found"));
        if (!plan.getPatientId().equals(patientId))
            throw denied();
        Map<UUID, Type> current = new HashMap<>();
        var ids = new HashSet<UUID>();
        plan.getMeals().forEach(m -> ids.add(m.id()));
        for (var event : repository.forPlan(planId)) {
            ids.add(event.plannedMealId());
            current.put(event.plannedMealId(), event.eventType());
        }
        int completed = 0, missed = 0, substituted = 0;
        for (UUID id : ids) {
            Type type = current.get(id);
            if (type == Type.COMPLETED)
                completed++;
            else if (type == Type.NOT_COMPLETED)
                missed++;
            else if (type == Type.SUBSTITUTED)
                substituted++;
        }
        return new Summary(
                planId, ids.size(), completed, missed, substituted,
                ids.size() - completed - missed - substituted
        );
    }

    private UUID requirePatient(Actor actor) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw denied();
        return references.patientId(actor.accountId()).orElseThrow(this::denied);
    }

    private TrackingException denied() {
        return new TrackingException(Kind.FORBIDDEN, "Tracking access denied");
    }

    private TrackingException invalid(String message) {
        return new TrackingException(Kind.INVALID, message);
    }

    private int checkedPage(int page) {
        if (page < 0)
            throw invalid("page must be nonnegative");
        return page;
    }

    private int checkedSize(int size) {
        if (size < 1 || size > 100)
            throw invalid("size must be between 1 and 100");
        return size;
    }

    private String encode(Object value) {
        try {
            return json.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private String hash(Create input) {
        // The key is scoped to the patient; all command fields, including the meal, are compared.
        String canonical = encode(
                new Object[] { input.plannedMealId(), input.reason(), input.occurredAt(),
                        input.consumedItems() }
        );
        try {
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(canonical.getBytes(StandardCharsets.UTF_8))
            );
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }

    private List<LoggedItem> resolveConsumed(
            PlanningMode mode, PlannedMeal meal,
            com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource source,
            List<ConsumedItem> input
    ) {
        var seenPlanned = new HashSet<UUID>();
        var resolved = new ArrayList<LoggedItem>();
        for (ConsumedItem item : input) {
            if (item == null || item.catalogFoodId() == null || item.catalogPortionId() == null
                    || item.unit() == null
                    || item.unit().isBlank() || item.quantity() == null || item.multiplier() == null
                    || item.quantity().signum() < 0 || item.multiplier().signum() <= 0)
                throw invalid("Each consumed item needs food, portion, unit, nonnegative quantity and positive multiplier");
            PlannedMealItem planned = item.plannedItemId() == null ? null : meal.items().stream()
                    .filter(candidate -> candidate.id().equals(item.plannedItemId())).findFirst()
                    .orElseThrow(() -> invalid("Planned item is unavailable"));
            if (planned != null && !seenPlanned.add(planned.id()))
                throw invalid("A planned item can appear only once in consumedItems");
            if (item.quantity().signum() == 0 && planned == null)
                throw invalid("Only a planned item can be removed with quantity zero");
            if (item.quantity().signum() == 0 && planned != null
                    && !planned.foodId().equals(item.catalogFoodId()))
                throw invalid("A removed planned item must keep its planned food");
            if (planned == null && item.quantity().signum() <= 0)
                throw invalid("A new free item needs a portion and positive quantity");
            if (mode != PlanningMode.FREE && planned == null)
                throw invalid("This planning mode requires a planned item");
            if (mode == PlanningMode.RESTRICTED && !planned.foodId().equals(item.catalogFoodId()))
                throw invalid("Restricted plans only allow the planned food");
            if (item.allowedSubstitutionId() != null) {
                if (mode != PlanningMode.SUBSTITUTIONS_ONLY)
                    throw invalid("Allowed substitutions are only valid in substitutions-only plans");
                if (planned == null)
                    throw invalid("A substitution must reference a planned item");
                var substitution = source.meals().stream().flatMap(candidate -> candidate.items().stream())
                        .filter(candidate -> candidate.id().equals(planned.prescribedMealItemId()))
                        .flatMap(candidate -> candidate.substitutions().stream())
                        .filter(candidate -> candidate.id().equals(item.allowedSubstitutionId())).findFirst()
                        .orElseThrow(() -> invalid("Substitution is not authorized"));
                if (!substitution.foodId().equals(item.catalogFoodId()))
                    throw invalid("Consumed food does not match the authorized substitution");
            } else if (mode == PlanningMode.SUBSTITUTIONS_ONLY
                    && !planned.foodId().equals(item.catalogFoodId())) {
                throw invalid("Substitutions-only plans require an authorized substitution");
            }
            var portion = checkedCatalogItem(item);
            if (portion.getUnit() != null && !portion.getUnit().equals(item.unit().trim()))
                throw invalid("Consumed unit does not match the catalog portion unit");
            resolved.add(logged(item, portion));
        }
        if (!input.isEmpty() && seenPlanned.size() != meal.items().size())
            throw invalid("A nonempty consumedItems list needs exactly one line for every planned item");
        return List.copyOf(resolved);
    }

    private FoodLogItem foodLogItem(
            PlannedMealItem item,
            com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource source,
            PlanningMode mode
    ) {
        var substitutions = mode != PlanningMode.SUBSTITUTIONS_ONLY ? List.<LoggedItem>of()
                : source.meals().stream().flatMap(meal -> meal.items().stream())
                        .filter(candidate -> candidate.id().equals(item.prescribedMealItemId()))
                        .flatMap(candidate -> candidate.substitutions().stream())
                        .map(substitution -> logged(
                                new ConsumedItem(substitution.foodId(), substitution.portionId(),
                                        substitution.unit(), substitution.quantity(), BigDecimal.ONE,
                                        item.id(), substitution.id()), catalogPortionOrNull(
                                                substitution.foodId(), substitution.portionId())
                        )).toList();
        var planned = logged(new ConsumedItem(item.foodId(), item.foodPortionId(), item.unit(),
                item.quantity(), BigDecimal.ONE, item.id(), null),
                catalogPortionOrNull(item.foodId(), item.foodPortionId()));
        return new FoodLogItem(planned.plannedItemId(), planned.catalogFoodId(),
                planned.catalogPortionId(), planned.unit(), planned.quantity(), planned.multiplier(),
                planned.canonicalConversion(), planned.kcalSnapshot(), substitutions);
    }

    private List<LoggedItem> plannedSnapshot(PlannedMeal meal) {
        return meal.items().stream().map(item -> logged(
                new ConsumedItem(item.foodId(), item.foodPortionId(), item.unit(), item.quantity(),
                        BigDecimal.ONE, item.id(), item.allowedSubstitutionId()),
                catalogPortionOrNull(item.foodId(), item.foodPortionId()))).toList();
    }

    private FoodPortion checkedCatalogItem(ConsumedItem item) {
        references.activeFood(item.catalogFoodId())
                .orElseThrow(() -> invalid("Catalog food is unavailable"));
        if (item.catalogPortionId() == null)
            throw invalid("Catalog portion is required");
        return references.activePortion(item.catalogFoodId(), item.catalogPortionId())
                .orElseThrow(() -> invalid("Catalog portion is unavailable for this food"));
    }

    private FoodPortion catalogPortionOrNull(UUID foodId, UUID portionId) {
        return portionId == null ? null : references.activePortion(foodId, portionId).orElse(null);
    }

    private LoggedItem logged(ConsumedItem item, FoodPortion portion) {
        BigDecimal multiplier = item.multiplier().stripTrailingZeros();
        BigDecimal quantity = item.quantity().stripTrailingZeros();
        BigDecimal factor = null;
        BigDecimal grams = null;
        if (portion != null && portion.getGramsEquivalent() != null) {
            factor = quantity.multiply(multiplier).divide(portion.getQuantity(), 12, RoundingMode.HALF_UP);
            grams = factor.multiply(portion.getGramsEquivalent()).stripTrailingZeros();
        } else if ("g".equalsIgnoreCase(item.unit().trim())) {
            grams = quantity.multiply(multiplier).stripTrailingZeros();
        }
        BigDecimal kcal = null;
        if (portion != null && factor != null) {
            var energy = portion.getNutrients().get("energy_kcal");
            if (energy != null && energy.amount() != null)
                kcal = energy.amount().multiply(factor).stripTrailingZeros();
        }
        return new LoggedItem(item.plannedItemId(), item.catalogFoodId(), item.catalogPortionId(),
                item.unit().trim(), quantity, multiplier,
                grams == null ? null : new CanonicalConversion(grams), kcal,
                item.allowedSubstitutionId());
    }

    private Event sameOrConflict(MealCompletionEvent event, String hash) {
        if (!event.commandHash().equals(hash))
            throw new TrackingException(Kind.CONFLICT, "Idempotency-Key was used with different content");
        try {
            return new Event(
                    event.id(), event.sequenceId(), event.patientId(), event.weeklyPlanId(),
                    event.plannedMealId(), event.eventType(), event.reason(), event.occurredAt(),
                    event.createdAt(), json.readValue(event.mealSnapshot(), Object.class),
                    json.readValue(event.plannedItemsSnapshot(), Object.class),
                    json.readValue(event.consumedItemsSnapshot(), Object.class)
            );
        } catch (JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }

    private PageResult<Event> map(PageResult<MealCompletionEvent> page) {
        return new PageResult<>(
                page.content().stream().map(e -> sameOrConflict(e, e.commandHash())).toList(),
                page.page(), page.size(), page.totalElements(), page.totalPages()
        );
    }
}
