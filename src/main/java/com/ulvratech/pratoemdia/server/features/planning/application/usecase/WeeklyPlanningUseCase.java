package com.ulvratech.pratoemdia.server.features.planning.application.usecase;

import com.ulvratech.pratoemdia.server.features.planning.application.dto.GeneratedPlan;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanGeneratedNotice;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.Actor;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.ItemInput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.MealInput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanQuery;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PlanUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PlanningInputs.PreferenceUpdate;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.PrescriptionPlanningSource.MealSource;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.dto.WeeklyPlanningPreferenceOutput;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.InvalidPlanningChangeException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningAccessDeniedException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.PlanningConflictException;
import com.ulvratech.pratoemdia.server.features.planning.application.exception.WeeklyPlanNotFoundException;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningCommands;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.PlanningQueries;
import com.ulvratech.pratoemdia.server.features.planning.application.port.in.ReconcileWeeklyPlanning;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanGeneratedNotifier;
import com.ulvratech.pratoemdia.server.features.planning.application.port.out.PlanningReferences;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMeal;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.PlannedMealItem;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlan;
import com.ulvratech.pratoemdia.server.features.planning.domain.entity.WeeklyPlanningPreference;
import com.ulvratech.pratoemdia.server.features.planning.domain.port.out.WeeklyPlanningRepository;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import jakarta.transaction.Transactional;
import java.time.Clock;
import java.time.DayOfWeek;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

@Service
@Transactional
@Slf4j
public class WeeklyPlanningUseCase
        implements PlanningCommands, PlanningQueries, ReconcileWeeklyPlanning {
    public static final ZoneId ZONE = ZoneId.of("America/Sao_Paulo");
    private final WeeklyPlanningRepository repository;
    private final PlanningReferences references;
    private final Clock clock;
    private final PlanGeneratedNotifier notifier;

    public WeeklyPlanningUseCase(
            WeeklyPlanningRepository repository, PlanningReferences references, Clock clock,
            PlanGeneratedNotifier notifier
    ) {
        this.repository = repository;
        this.references = references;
        this.clock = clock;
        this.notifier = notifier;
    }

    @Override
    public WeeklyPlanningPreferenceOutput preference(Actor actor) {
        UUID patient = requirePatient(actor);
        return WeeklyPlanningPreferenceOutput.from(
                repository
                        .findPreference(patient)
                        .orElse(new WeeklyPlanningPreference(patient, DayOfWeek.MONDAY, false))
        );
    }

    @Override
    public WeeklyPlanningPreferenceOutput updatePreference(PreferenceUpdate input) {
        UUID patient = requirePatient(input.actor());
        return WeeklyPlanningPreferenceOutput.from(
                repository.savePreference(
                        new WeeklyPlanningPreference(patient, input.dayOfWeek(), input.enabled())
                )
        );
    }

    @Override
    public WeeklyPlanOutput generate(Actor actor) {
        UUID patient = requirePatient(actor);
        var generated = generateFor(patient, LocalDate.now(clock.withZone(ZONE)), true);
        notifyGenerated(generated, false);
        return WeeklyPlanOutput.from(generated.plan());
    }

    @Override
    public List<WeeklyPlanOutput> list(Actor actor) {
        return repository.findPlans(requirePatient(actor)).stream()
                .map(WeeklyPlanOutput::from)
                .toList();
    }

    @Override
    public WeeklyPlanOutput get(PlanQuery input) {
        return WeeklyPlanOutput.from(requireOwned(input.planId(), input.actor()));
    }

    @Override
    public WeeklyPlanOutput getForNutritionist(Actor actor, UUID patientId, UUID planId) {
        if (actor == null || !"NUTRITIONIST".equals(actor.role())
                || !references.hasActiveRelationship(actor.userAccountId(), patientId))
            throw new PlanningAccessDeniedException();
        var plan = repository.findPlan(planId).orElseThrow(WeeklyPlanNotFoundException::new);
        if (!plan.getPatientId().equals(patientId))
            throw new PlanningAccessDeniedException();
        return WeeklyPlanOutput.from(plan);
    }

    @Override
    public WeeklyPlanOutput confirm(PlanQuery input) {
        var plan = requireOwned(input.planId(), input.actor());
        plan.confirm(Instant.now(clock));
        return WeeklyPlanOutput.from(repository.savePlan(plan));
    }

    @Override
    public void delete(PlanQuery input) {
        var plan = requireOwned(input.planId(), input.actor());
        repository.deactivatePlan(plan.getId());
    }

    @Override
    public WeeklyPlanOutput update(PlanUpdate input) {
        var plan = requireOwned(input.planId(), input.actor());
        if (input.version() != plan.getPersistenceVersion())
            throw new PlanningConflictException("Weekly plan was changed");
        try {
            plan.replaceMeals(desiredMeals(plan, input.meals()), Instant.now(clock));
        } catch (IllegalArgumentException | IllegalStateException exception) {
            throw new InvalidPlanningChangeException(exception.getMessage());
        }
        try {
            return WeeklyPlanOutput.from(repository.savePlan(plan));
        } catch (org.springframework.orm.ObjectOptimisticLockingFailureException exception) {
            throw new PlanningConflictException("Weekly plan was changed");
        }
    }

    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void generateDuePlans() {
        var now = ZonedDateTime.now(clock.withZone(ZONE));
        if (now.toLocalTime().isBefore(LocalTime.of(8, 0)))
            return;
        repository
                .findEnabledPreferences(now.getDayOfWeek())
                .forEach(
                        preference -> {
                            try {
                                notifyGenerated(generateFor(preference.patientId(), now.toLocalDate(), false), true);
                            } catch (WeeklyPlanNotFoundException | PlanningConflictException ignored) {
                            }
                        }
                );
    }

    @Override
    @Transactional(Transactional.TxType.NOT_SUPPORTED)
    public void confirmDuePlans() {
        Instant now = Instant.now(clock);
        repository
                .findPendingDue(now)
                .forEach(
                        plan -> {
                            plan.confirm(now);
                            repository.savePlan(plan);
                        }
                );
    }

    private void notifyGenerated(GeneratedPlan generated, boolean automatic) {
        if (!generated.created())
            return;
        try {
            notifier.planGenerated(PlanGeneratedNotice.from(generated.plan(), automatic));
        } catch (RuntimeException error) {
            log.error("Failed to notify plan generation. planId={}", generated.plan().getId(), error);
        }
    }

    private GeneratedPlan generateFor(UUID patient, LocalDate generatedOn, boolean conflictWhenExists) {
        LocalDate start = generatedOn.plusDays(1), end = start.plusDays(6);
        if (repository.existsForPeriod(patient, start, end)) {
            if (conflictWhenExists)
                throw new PlanningConflictException("Weekly plan already exists for this period");
            return new GeneratedPlan(
                    repository.findPlans(patient).stream()
                            .filter(p -> p.getPeriodStart().equals(start))
                            .findFirst()
                            .orElseThrow(
                                    () -> new PlanningConflictException(
                                            "Weekly plan already exists for this period"
                                    )
                            ),
                    false
            );
        }
        var source = references
                .currentPublishedPrescription(patient)
                .orElseThrow(WeeklyPlanNotFoundException::new);
        var meals = new ArrayList<PlannedMeal>();
        for (int day = 0; day < 7; day++) {
            LocalDate date = start.plusDays(day);
            for (var meal : source.meals())
                meals.add(copy(meal, date));
        }
        Instant deadline = start.atStartOfDay(ZONE).toInstant();
        try {
            return new GeneratedPlan(
                    repository.savePlan(
                            WeeklyPlan.create(
                                    patient, source.versionId(), source.planningMode(), start, deadline, meals
                            )
                    ),
                    true
            );
        } catch (DataIntegrityViolationException exception) {
            throw new PlanningConflictException("Weekly plan already exists for this period");
        }
    }

    private PlannedMeal copy(MealSource meal, LocalDate date) {
        return new PlannedMeal(
                UUID.randomUUID(),
                meal.id(),
                date,
                meal.time(),
                meal.mealType(),
                meal.name(),
                meal.order(),
                meal.items().stream()
                        .map(
                                item -> new PlannedMealItem(
                                        UUID.randomUUID(),
                                        item.id(),
                                        null,
                                        item.foodId(),
                                        item.portionId(),
                                        item.quantity(),
                                        item.unit(),
                                        item.order()
                                )
                        )
                        .toList()
        );
    }

    private List<PlannedMeal> desiredMeals(WeeklyPlan plan, List<MealInput> inputs) {
        var existingMeals = plan.getMeals().stream()
                .collect(java.util.stream.Collectors.toMap(PlannedMeal::id, m -> m));
        return inputs.stream()
                .map(
                        input -> {
                            var oldMeal = input.id() == null ? null : existingMeals.get(input.id());
                            UUID mealId = input.id() == null ? UUID.randomUUID() : input.id();
                            UUID prescribedMealId = oldMeal == null ? null : oldMeal.prescribedMealId();
                            var oldItems = oldMeal == null
                                    ? Map.<UUID, PlannedMealItem>of()
                                    : oldMeal.items().stream()
                                            .collect(java.util.stream.Collectors.toMap(PlannedMealItem::id, i -> i));
                            var items = input.items().stream().map(item -> desiredItem(plan, item, oldItems)).toList();
                            return new PlannedMeal(
                                    mealId,
                                    prescribedMealId,
                                    input.date(),
                                    input.time(),
                                    input.mealType(),
                                    input.name(),
                                    input.order(),
                                    items
                            );
                        }
                )
                .toList();
    }

    private PlannedMealItem desiredItem(
            WeeklyPlan plan, ItemInput input, Map<UUID, PlannedMealItem> existing
    ) {
        var old = input.id() == null ? null : existing.get(input.id());
        if (plan.getPlanningMode() == PlanningMode.SUBSTITUTIONS_ONLY) {
            if (old == null || old.prescribedMealItemId() == null)
                throw new InvalidPlanningChangeException("Items cannot be added or removed");
            var source = references
                    .prescriptionVersion(plan.getPrescriptionVersionId())
                    .flatMap(
                            p -> p.meals().stream()
                                    .flatMap(m -> m.items().stream())
                                    .filter(i -> i.id().equals(old.prescribedMealItemId()))
                                    .findFirst()
                    )
                    .orElseThrow(
                            () -> new InvalidPlanningChangeException("Prescribed item is unavailable")
                    );
            if (matches(input, source.foodId(), source.portionId())) {
                requireCanonical(input, source.quantity(), source.unit(), source.order());
                return new PlannedMealItem(
                        old.id(),
                        source.id(),
                        null,
                        source.foodId(),
                        source.portionId(),
                        source.quantity(),
                        source.unit(),
                        source.order()
                );
            }
            var substitution = source.substitutions().stream()
                    .filter(s -> matches(input, s.foodId(), s.portionId()))
                    .findFirst()
                    .orElseThrow(
                            () -> new InvalidPlanningChangeException("Substitution is not authorized")
                    );
            requireCanonical(input, substitution.quantity(), substitution.unit(), source.order());
            return new PlannedMealItem(
                    old.id(),
                    source.id(),
                    substitution.id(),
                    substitution.foodId(),
                    substitution.portionId(),
                    substitution.quantity(),
                    substitution.unit(),
                    source.order()
            );
        }
        if (!references.isActiveFood(input.foodId()))
            throw new InvalidPlanningChangeException("Food is missing or inactive");
        if (input.foodPortionId() != null
                && !references.portionBelongsToFood(input.foodPortionId(), input.foodId()))
            throw new InvalidPlanningChangeException("Food portion does not belong to the selected food");
        return new PlannedMealItem(
                input.id() == null ? UUID.randomUUID() : input.id(),
                old == null ? null : old.prescribedMealItemId(),
                null,
                input.foodId(),
                input.foodPortionId(),
                input.quantity(),
                input.unit(),
                input.order()
        );
    }

    private boolean matches(ItemInput input, UUID food, UUID portion) {
        return Objects.equals(input.foodId(), food) && Objects.equals(input.foodPortionId(), portion);
    }

    private void requireCanonical(
            ItemInput input, java.math.BigDecimal quantity, String unit, int order
    ) {
        if (input.quantity() == null
                || input.quantity().compareTo(quantity) != 0
                || !Objects.equals(input.unit(), unit)
                || input.order() != order)
            throw new InvalidPlanningChangeException(
                    "Substitution quantity, unit and order cannot be changed"
            );
    }

    private WeeklyPlan requireOwned(UUID id, Actor actor) {
        UUID patient = requirePatient(actor);
        var plan = repository.findPlan(id).orElseThrow(WeeklyPlanNotFoundException::new);
        if (!plan.getPatientId().equals(patient))
            throw new PlanningAccessDeniedException();
        return plan;
    }

    private UUID requirePatient(Actor actor) {
        if (actor == null || !"PATIENT".equals(actor.role()))
            throw new PlanningAccessDeniedException();
        return references
                .patientProfileId(actor.userAccountId())
                .orElseThrow(PlanningAccessDeniedException::new);
    }
}
