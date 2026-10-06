package com.ulvratech.pratoemdia.server.features.tracking.application.usecase;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Actor;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Choice;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Create;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.TrackingData.Event;
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
        if (input.eventType() == null || input.occurredAt() == null)
            throw invalid("eventType and occurredAt are required");
        try {
            MealCompletionEvent.validateChoices(input.eventType(), input.choices());
        } catch (IllegalArgumentException e) {
            throw invalid(e.getMessage());
        }
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
        List<PlannedMealItem> consumed = new ArrayList<>(meal.items());
        if (input.eventType() == Type.SUBSTITUTED) {
            var seen = new HashSet<UUID>();
            var source = references.prescriptionVersion(plan.getPrescriptionVersionId())
                    .orElseThrow(() -> invalid("Prescription version is unavailable"));
            for (Choice choice : input.choices()) {
                if (choice == null || choice.plannedMealItemId() == null
                        || choice.allowedSubstitutionId() == null
                        || !seen.add(choice.plannedMealItemId()))
                    throw invalid("Each planned item needs one distinct substitution");
                var item = meal.items().stream().filter(i -> i.id().equals(choice.plannedMealItemId()))
                        .findFirst().orElseThrow(() -> invalid("Planned item is unavailable"));
                var prescribed = source.meals().stream().flatMap(m -> m.items().stream())
                        .filter(i -> i.id().equals(item.prescribedMealItemId())).findFirst()
                        .orElseThrow(() -> invalid("Prescribed item is unavailable"));
                var substitute = prescribed.substitutions().stream()
                        .filter(s -> s.id().equals(choice.allowedSubstitutionId())).findFirst()
                        .orElseThrow(() -> invalid("Substitution is not authorized"));
                consumed.replaceAll(
                        i -> i.id().equals(item.id())
                                ? new PlannedMealItem(
                                        i.id(), i.prescribedMealItemId(), substitute.id(),
                                        substitute.foodId(), substitute.portionId(), substitute.quantity(),
                                        substitute.unit(), i.order()
                                )
                                : i
                );
            }
        }
        var candidate = new MealCompletionEvent(
                0, UUID.randomUUID(), patient, plan.getId(),
                meal.id(), input.eventType(), input.reason(), input.occurredAt(), now,
                input.idempotencyKey(), hash, encode(meal), encode(meal.items()),
                encode(input.eventType() == Type.NOT_COMPLETED ? List.of() : consumed)
        );
        var saved = repository.append(candidate);
        return sameOrConflict(saved, hash);
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
                new Object[] { input.plannedMealId(), input.eventType(),
                        input.reason(), input.occurredAt(), input.choices() }
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
