package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.catalog.infrastructure.persistence.adapter.CatalogQueryAdapter;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.PlanningMode;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Deleted;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Execution;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.ItemInput;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Write;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.ExecutionData;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.mapper.MealExecutionMapper;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionCommandModel;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionItemModel;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionModel;
import com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.model.MealExecutionNutrientSnapshotModel;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalTime;
import java.time.temporal.ChronoUnit;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExecutionDataAdapter implements ExecutionData {
    private static final int SCALE_QUANTITY = 3;
    private static final int SCALE_AMOUNT = 4;

    private final MealExecutionJpaRepository executions;
    private final MealExecutionCommandJpaRepository commands;
    private final ExecutionLookups lookups;
    private final MealExecutionMapper mapper;
    private final ObjectMapper json;
    private final Clock clock;

    ExecutionDataAdapter(
            MealExecutionJpaRepository executions, MealExecutionCommandJpaRepository commands,
            ExecutionLookups lookups, MealExecutionMapper mapper, ObjectMapper json, Clock clock
    ) {
        this.executions = executions;
        this.commands = commands;
        this.lookups = lookups;
        this.mapper = mapper;
        this.json = json;
        this.clock = clock;
    }

    private record Context(UUID planId, UUID versionId, PlanningMode mode, LocalDate mealDate,
            LocalTime mealTime, MealType mealType) {
    }

    private record Slot(LocalTime time, MealType type) {
    }

    @Override
    @Transactional
    public Execution create(UUID patientId, String key, Write input) {
        requireKey(key);
        requireWrite(input);
        String hash = hash("create", input);
        var repeated = repeated(patientId, key, hash);
        if (repeated != null)
            return repeatedExecution(patientId, key, repeated);
        var context = context(patientId, input.date(), input.plannedMealId());
        validateItems(context, input.plannedMealId(), input.items());
        var slot = slot(context, input, null, null);
        var now = now();
        var model = MealExecutionModel.builder()
                .id(UUID.randomUUID()).patientId(patientId).date(input.date())
                .time(slot.time()).mealType(slot.type()).plannedMealId(input.plannedMealId())
                .weeklyPlanId(context.planId()).prescriptionVersionId(context.versionId())
                .mode(context.mode()).version(1).createdAt(now).updatedAt(now).build();
        saveItems(model, input.items());
        try {
            model = executions.saveAndFlush(model);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Destination or idempotency key is already in use");
        }
        var result = mapper.toExecution(model);
        saveCommand(patientId, key, hash, model.getId(), 1, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Execution get(UUID patientId, UUID id) {
        return mapper.toExecution(find(patientId, id));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Execution> list(UUID patientId, LocalDate date) {
        if (date == null)
            throw invalid("Date is required");
        return executions.findAllByPatientIdAndDateAndDeletedAtIsNullOrderByCreatedAtAscIdAsc(patientId, date)
                .stream().map(mapper::toExecution).toList();
    }

    @Override
    @Transactional
    public Execution replace(UUID patientId, UUID id, long expectedVersion, String key, Write input) {
        requireKey(key);
        requireWrite(input);
        var hash = hash("replace:" + id + ":" + expectedVersion, input);
        var repeated = repeated(patientId, key, hash);
        if (repeated != null)
            return repeatedExecution(patientId, key, repeated);
        var model = findForUpdate(patientId, id);
        requireCurrent(model, expectedVersion);
        if (!model.getDate().equals(input.date())
                || !Objects.equals(model.getPlannedMealId(), input.plannedMealId()))
            throw conflict("Use move to change the execution destination");
        var context = context(patientId, input.date(), input.plannedMealId());
        validateItems(context, input.plannedMealId(), input.items());
        var slot = slot(context, input, model.getTime(), model.getMealType());
        model.setTime(slot.time());
        model.setMealType(slot.type());
        touch(model);
        replaceItems(model, input.items());
        executions.saveAndFlush(model);
        var result = mapper.toExecution(model);
        saveCommand(patientId, key, hash, id, model.getVersion(), result);
        return result;
    }

    @Override
    @Transactional
    public Execution move(
            UUID patientId, UUID id, long expectedVersion, String key,
            Write destination
    ) {
        requireKey(key);
        if (destination == null || destination.date() == null)
            throw invalid("Destination date is required");
        var date = destination.date();
        var plannedMealId = destination.plannedMealId();
        var hash = hash("move:" + id + ":" + expectedVersion, destination);
        var repeated = repeated(patientId, key, hash);
        if (repeated != null)
            return repeatedExecution(patientId, key, repeated);
        var model = findForUpdate(patientId, id);
        requireCurrent(model, expectedVersion);
        var before = mapper.toExecution(model);
        var context = context(patientId, date, plannedMealId);
        var items = destination.items() != null ? destination.items()
                : before.items().stream()
                        .map(
                                item -> new ItemInput(
                                        item.foodId(), item.portionId(), item.unit(),
                                        item.quantity(), item.multiplier(),
                                        plannedMealId == null ? null : item.plannedItemId(),
                                        plannedMealId == null ? null : item.allowedSubstitutionId()
                                )
                        )
                        .toList();
        validateItems(context, plannedMealId, items);
        var slot = slot(context, destination, before.time(), before.mealType());
        model.setDate(date);
        model.setTime(slot.time());
        model.setMealType(slot.type());
        model.setPlannedMealId(plannedMealId);
        model.setWeeklyPlanId(context.planId());
        model.setPrescriptionVersionId(context.versionId());
        model.setMode(context.mode());
        touch(model);
        try {
            if (destination.items() != null) {
                replaceItems(model, items);
            } else if (plannedMealId == null) {
                model.getItems().forEach(item -> {
                    item.setPlannedItemId(null);
                    item.setAllowedSubstitutionId(null);
                });
            }
            executions.saveAndFlush(model);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Destination already has an execution");
        }
        var result = mapper.toExecution(model);
        saveCommand(patientId, key, hash, id, model.getVersion(), result);
        return result;
    }

    @Override
    @Transactional
    public Deleted delete(UUID patientId, UUID id, long expectedVersion, String key) {
        requireKey(key);
        var hash = hash("delete", new Object[] { id, expectedVersion });
        var repeated = repeated(patientId, key, hash);
        if (repeated != null)
            return repeatedDeleted(patientId, key, repeated);
        var model = findForUpdate(patientId, id);
        requireCurrent(model, expectedVersion);
        var now = now();
        model.setDeletedAt(now);
        model.setRestoreToken(UUID.randomUUID());
        model.setRestoreExpiresAt(now.plusSeconds(5));
        touch(model);
        executions.saveAndFlush(model);
        var result = mapper.toDeleted(model);
        saveCommand(patientId, key, hash, id, model.getVersion(), result);
        return result;
    }

    @Override
    @Transactional
    public Execution restore(UUID patientId, UUID id, UUID token) {
        if (token == null)
            throw invalid("Restore token is required");
        var model = findForUpdate(patientId, id);
        if (!token.equals(model.getRestoreToken()))
            throw conflict("Restore token is invalid");
        var now = now();
        if (model.getRestoreExpiresAt() == null || now.isAfter(model.getRestoreExpiresAt()))
            throw conflict("Restore window has expired");
        if (model.getDeletedAt() == null)
            return mapper.toExecution(model);
        model.setDeletedAt(null);
        touch(model);
        try {
            executions.saveAndFlush(model);
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Destination already has an execution");
        }
        return mapper.toExecution(model);
    }

    private MealExecutionModel find(UUID patientId, UUID id) {
        return executions.findByIdAndPatientId(id, patientId)
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Execution was not found"));
    }

    private MealExecutionModel findForUpdate(UUID patientId, UUID id) {
        return executions.findForUpdate(id, patientId)
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Execution was not found"));
    }

    private void requireCurrent(MealExecutionModel model, long expectedVersion) {
        if (model.getDeletedAt() != null || model.getVersion() != expectedVersion)
            throw conflict("Execution version is stale");
    }

    private void touch(MealExecutionModel model) {
        model.setVersion(model.getVersion() + 1);
        model.setUpdatedAt(now());
    }

    private Instant now() {
        return clock.instant().truncatedTo(ChronoUnit.MICROS);
    }

    private Context context(UUID patientId, LocalDate date, UUID plannedMealId) {
        if (plannedMealId == null) {
            return lookups.publishedFreeVersion(patientId)
                    .map(version -> new Context(null, version, PlanningMode.FREE, null, null, null))
                    .orElseThrow(() -> invalid("A current published FREE prescription is required"));
        }
        var found = lookups.planSlot(plannedMealId, patientId)
                .map(
                        slot -> new Context(
                                slot.planId(), slot.versionId(), slot.mode(), slot.mealDate(),
                                slot.mealTime(), slot.mealType()
                        )
                )
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Planned meal was not found"));
        if (!date.equals(found.mealDate()))
            throw invalid("Execution date must match the planned meal date");
        return found;
    }

    private Slot slot(Context context, Write input, LocalTime previousTime, MealType previousType) {
        var type = input.mealType();
        if (context.mealType() != null) {
            if (type != null && type != context.mealType())
                throw invalid("Meal type must match the planned meal");
            type = context.mealType();
        } else if (type == null) {
            type = previousType;
        }
        var time = input.time();
        if (time == null)
            time = context.mealTime() != null ? context.mealTime() : previousTime;
        if (type == null || time == null)
            throw invalid("Meal time and meal type are required");
        return new Slot(time, type);
    }

    private void validateItems(Context context, UUID mealId, List<ItemInput> items) {
        Set<UUID> seen = new HashSet<>();
        boolean free = context.mode() == PlanningMode.FREE;
        for (var item : items) {
            if (item == null || item.foodId() == null || item.portionId() == null
                    || item.unit() == null || item.unit().isBlank() || item.quantity() == null
                    || item.quantity().signum() <= 0 || item.multiplier() == null
                    || item.multiplier().signum() <= 0)
                throw invalid("Each item needs food, portion, unit and positive quantity and multiplier");
            if (item.plannedItemId() != null && !seen.add(item.plannedItemId()))
                throw invalid("A planned item may appear only once");
            if (item.plannedItemId() == null && !free)
                throw invalid("This mode requires planned items");
            if (item.plannedItemId() != null) {
                var planned = lookups.plannedItem(item.plannedItemId(), mealId)
                        .orElseThrow(() -> invalid("Planned item is unavailable"));
                if (context.mode() == PlanningMode.RESTRICTED && !planned.foodId().equals(item.foodId()))
                    throw invalid("Restricted executions must keep planned foods");
                if (context.mode() == PlanningMode.SUBSTITUTIONS_ONLY
                        && !planned.foodId().equals(item.foodId())) {
                    if (item.allowedSubstitutionId() == null || planned.prescribedItemId() == null
                            || !lookups.substitutionAuthorized(
                                    item.allowedSubstitutionId(), planned.prescribedItemId(),
                                    item.foodId(), item.portionId()
                            ))
                        throw invalid("Substitution is not authorized");
                } else if (item.allowedSubstitutionId() != null)
                    throw invalid("Substitution reference is not applicable");
            } else if (item.allowedSubstitutionId() != null)
                throw invalid("Substitution requires a planned item");
            portion(item);
        }
        if (!free && !items.isEmpty() && seen.size() != lookups.plannedItemCount(mealId))
            throw invalid("Every planned item must be represented");
    }

    private CatalogQueryAdapter.Portion portion(ItemInput item) {
        var portion = lookups.activePortion(item.foodId(), item.portionId())
                .orElseThrow(() -> invalid("Food or portion is missing or inactive"));
        if (portion.unit() != null && !portion.unit().equals(item.unit().trim()))
            throw invalid("Unit does not match the selected portion");
        return portion;
    }

    private void replaceItems(MealExecutionModel model, List<ItemInput> inputs) {
        model.getItems().clear();
        executions.flush();
        saveItems(model, inputs);
    }

    private void saveItems(MealExecutionModel model, List<ItemInput> inputs) {
        int order = 0;
        for (var input : inputs) {
            var portion = portion(input);
            var consumed = input.quantity().multiply(input.multiplier());
            var factor = consumed.divide(portion.quantity(), 12, RoundingMode.HALF_UP);
            var grams = portion.gramsEquivalent() == null ? null
                    : portion.gramsEquivalent().multiply(factor).setScale(SCALE_AMOUNT, RoundingMode.HALF_UP);
            var item = MealExecutionItemModel.builder()
                    .id(UUID.randomUUID()).execution(model).order(order++)
                    .plannedItemId(input.plannedItemId())
                    .allowedSubstitutionId(input.allowedSubstitutionId())
                    .foodId(input.foodId()).foodPortionId(input.portionId())
                    .quantity(input.quantity().setScale(SCALE_QUANTITY, RoundingMode.HALF_UP))
                    .multiplier(input.multiplier().setScale(SCALE_QUANTITY, RoundingMode.HALF_UP))
                    .unit(input.unit().trim()).grams(grams).build();
            for (var nutrient : portion.nutrients()) {
                if (nutrient.amount() == null)
                    continue;
                if ("energy_kcal".equals(nutrient.code()))
                    item.setKcal(nutrient.amount().multiply(factor).setScale(SCALE_AMOUNT, RoundingMode.HALF_UP));
                item.getNutrients().add(
                        MealExecutionNutrientSnapshotModel.builder()
                                .item(item).nutrientCode(nutrient.code())
                                .amount(
                                        nutrient.amount().multiply(consumed)
                                                .divide(portion.quantity(), SCALE_AMOUNT, RoundingMode.HALF_UP)
                                )
                                .unit(nutrient.unit()).build()
                );
            }
            model.getItems().add(item);
        }
    }

    private void requireWrite(Write input) {
        if (input == null || input.date() == null || input.items() == null)
            throw invalid("Date and items are required");
    }

    private void requireKey(String key) {
        if (key == null || key.isBlank() || key.length() > 255)
            throw invalid("Idempotency-Key is required");
    }

    private UUID repeated(UUID patientId, String key, String hash) {
        var command = commands.findById(new MealExecutionCommandModel.Key(patientId, key));
        if (command.isEmpty())
            return null;
        if (!command.get().getCommandHash().equals(hash))
            throw conflict("Idempotency-Key was used with different content");
        return command.get().getExecutionId();
    }

    private void saveCommand(
            UUID patientId, String key, String hash, UUID id, long version, Object result
    ) {
        if (commands.existsById(new MealExecutionCommandModel.Key(patientId, key)))
            throw conflict("Idempotency-Key is already in use");
        try {
            commands.saveAndFlush(
                    MealExecutionCommandModel.builder()
                            .patientId(patientId).idempotencyKey(key).commandHash(hash)
                            .executionId(id).resultVersion(version).createdAt(now())
                            .responseJson(json.writeValueAsString(result)).build()
            );
        } catch (DataIntegrityViolationException exception) {
            throw conflict("Idempotency-Key is already in use");
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private Execution repeatedExecution(UUID patientId, String key, UUID id) {
        var saved = response(patientId, key, Execution.class);
        return saved == null ? get(patientId, id) : saved;
    }

    private Deleted repeatedDeleted(UUID patientId, String key, UUID id) {
        var saved = response(patientId, key, Deleted.class);
        return saved == null ? mapper.toDeleted(find(patientId, id)) : saved;
    }

    private <T> T response(UUID patientId, String key, Class<T> type) {
        var payload = commands.findById(new MealExecutionCommandModel.Key(patientId, key))
                .map(MealExecutionCommandModel::getResponseJson).orElse(null);
        if (payload == null)
            return null;
        try {
            return json.readValue(payload, type);
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private String hash(String operation, Object input) {
        try {
            String encoded = operation + ":" + json.writeValueAsString(input);
            return HexFormat.of().formatHex(
                    MessageDigest.getInstance("SHA-256")
                            .digest(encoded.getBytes(StandardCharsets.UTF_8))
            );
        } catch (JsonProcessingException | NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }

    private TrackingException invalid(String message) {
        return new TrackingException(Kind.INVALID, message);
    }

    private TrackingException conflict(String message) {
        return new TrackingException(Kind.CONFLICT, message);
    }
}
