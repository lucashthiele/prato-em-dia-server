package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException;
import com.ulvratech.pratoemdia.server.features.tracking.application.exception.TrackingException.Kind;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.DiaryTypes.NutrientTotal;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Deleted;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Execution;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Item;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.ItemInput;
import com.ulvratech.pratoemdia.server.features.tracking.application.dto.ExecutionTypes.Write;
import com.ulvratech.pratoemdia.server.features.tracking.application.port.out.ExecutionData;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.time.Clock;
import com.ulvratech.pratoemdia.server.features.prescriptions.domain.enums.MealType;
import java.time.LocalDate;
import java.time.LocalTime;
import java.util.HashMap;
import java.util.HashSet;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class ExecutionDataAdapter implements ExecutionData {
    private final JdbcTemplate jdbc;
    private final ObjectMapper json;
    private final Clock clock;

    public ExecutionDataAdapter(JdbcTemplate jdbc, ObjectMapper json, Clock clock) {
        this.jdbc = jdbc;
        this.json = json;
        this.clock = clock;
    }

    private record Context(UUID planId, UUID versionId, String mode, LocalDate mealDate,
            LocalTime mealTime, MealType mealType) {
    }

    private record Slot(LocalTime time, MealType type) {
    }

    private record Portion(BigDecimal quantity, String unit, BigDecimal gramsEquivalent) {
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
        var id = UUID.randomUUID();
        var now = clock.instant();
        try {
            jdbc.update(
                    "insert into meal_execution(id,patient_id,execution_date,meal_time,meal_type,planned_meal_id,weekly_plan_id,prescription_version_id,mode,created_at,updated_at) "
                            + "values (?,?,?,?,?::meal_type,?,?,?,?::planning_mode,?,?)",
                    id, patientId, input.date(), slot.time(), slot.type().name(),
                    input.plannedMealId(), context.planId(),
                    context.versionId(), context.mode(), java.sql.Timestamp.from(now),
                    java.sql.Timestamp.from(now)
            );
            saveItems(id, input.items());
            jdbc.update(
                    "insert into meal_execution_command(patient_id,idempotency_key,command_hash,execution_id,result_version) values (?,?,?,?,1)",
                    patientId, key, hash, id
            );
        } catch (DuplicateKeyException exception) {
            throw conflict("Destination or idempotency key is already in use");
        }
        var result = get(patientId, id);
        saveResponse(patientId, key, result);
        return result;
    }

    @Override
    @Transactional(readOnly = true)
    public Execution get(UUID patientId, UUID id) {
        var rows = jdbc.query(
                "select id,version,execution_date,meal_time,meal_type::text,planned_meal_id,weekly_plan_id,prescription_version_id,mode,created_at,updated_at,deleted_at,restore_expires_at "
                        + "from meal_execution where patient_id=? and id=?",
                (rs, row) -> execution(rs, patientId), patientId, id
        );
        if (rows.isEmpty())
            throw new TrackingException(Kind.NOT_FOUND, "Execution was not found");
        return rows.getFirst();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Execution> list(UUID patientId, LocalDate date) {
        if (date == null)
            throw invalid("Date is required");
        return jdbc.query(
                "select id,version,execution_date,meal_time,meal_type::text,planned_meal_id,weekly_plan_id,prescription_version_id,mode,created_at,updated_at,deleted_at,restore_expires_at "
                        + "from meal_execution where patient_id=? and execution_date=? and deleted_at is null order by created_at,id",
                (rs, row) -> execution(rs, patientId), patientId, date
        );
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
        var before = get(patientId, id);
        if (before.deletedAt() != null || before.version() != expectedVersion)
            throw conflict("Execution version is stale");
        if (!before.date().equals(input.date())
                || !java.util.Objects.equals(before.plannedMealId(), input.plannedMealId()))
            throw conflict("Use move to change the execution destination");
        var context = context(patientId, input.date(), input.plannedMealId());
        validateItems(context, input.plannedMealId(), input.items());
        var slot = slot(context, input, before.time(), before.mealType());
        if (jdbc.update(
                "update meal_execution set meal_time=?,meal_type=?::meal_type,version=version+1,updated_at=? "
                        + "where id=? and patient_id=? and version=? and deleted_at is null",
                slot.time(), slot.type().name(),
                java.sql.Timestamp.from(clock.instant()), id, patientId, expectedVersion
        ) != 1)
            throw conflict("Execution version is stale");
        jdbc.update("delete from meal_execution_item where execution_id=?", id);
        saveItems(id, input.items());
        saveCommand(patientId, key, hash, id, expectedVersion + 1);
        var result = get(patientId, id);
        saveResponse(patientId, key, result);
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
        var before = get(patientId, id);
        if (before.deletedAt() != null || before.version() != expectedVersion)
            throw conflict("Execution version is stale");
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
        try {
            if (jdbc.update(
                    "update meal_execution set execution_date=?,meal_time=?,meal_type=?::meal_type,"
                            + "planned_meal_id=?,weekly_plan_id=?,"
                            + "prescription_version_id=?,mode=?::planning_mode,version=version+1,updated_at=? "
                            + "where id=? and patient_id=? and version=? and deleted_at is null",
                    date, slot.time(), slot.type().name(), plannedMealId, context.planId(), context.versionId(),
                    context.mode(),
                    java.sql.Timestamp.from(clock.instant()), id, patientId, expectedVersion
            ) != 1)
                throw conflict("Execution version is stale");
            if (destination.items() != null) {
                jdbc.update("delete from meal_execution_item where execution_id=?", id);
                saveItems(id, items);
            } else if (plannedMealId == null) {
                jdbc.update(
                        "update meal_execution_item set planned_item_id=null,allowed_substitution_id=null "
                                + "where execution_id=?",
                        id
                );
            }
            saveCommand(patientId, key, hash, id, expectedVersion + 1);
        } catch (DuplicateKeyException exception) {
            throw conflict("Destination already has an execution");
        }
        var result = get(patientId, id);
        saveResponse(patientId, key, result);
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
        var before = get(patientId, id);
        if (before.deletedAt() != null || before.version() != expectedVersion)
            throw conflict("Execution version is stale");
        var now = clock.instant();
        var token = UUID.randomUUID();
        var expires = now.plusSeconds(5);
        if (jdbc.update(
                "update meal_execution set deleted_at=?,restore_token=?,restore_expires_at=?,"
                        + "version=version+1,updated_at=? where id=? and patient_id=? and version=? and deleted_at is null",
                java.sql.Timestamp.from(now), token, java.sql.Timestamp.from(expires),
                java.sql.Timestamp.from(now), id, patientId, expectedVersion
        ) != 1)
            throw conflict("Execution version is stale");
        saveCommand(patientId, key, hash, id, expectedVersion + 1);
        var result = new Deleted(id, expectedVersion + 1, token, expires);
        saveResponse(patientId, key, result);
        return result;
    }

    @Override
    @Transactional
    public Execution restore(UUID patientId, UUID id, UUID token) {
        if (token == null)
            throw invalid("Restore token is required");
        var before = get(patientId, id);
        var existing = deletion(patientId, id);
        if (!token.equals(existing.restoreToken()))
            throw conflict("Restore token is invalid");
        var now = clock.instant();
        if (existing.restoreExpiresAt() == null || now.isAfter(existing.restoreExpiresAt()))
            throw conflict("Restore window has expired");
        if (before.deletedAt() == null)
            return before;
        try {
            if (jdbc.update(
                    "update meal_execution set deleted_at=null,version=version+1,updated_at=? "
                            + "where id=? and patient_id=? and version=? and deleted_at is not null "
                            + "and restore_token=? and restore_expires_at>=?",
                    java.sql.Timestamp.from(now), id, patientId, before.version(), token,
                    java.sql.Timestamp.from(now)
            ) != 1)
                throw conflict("Execution changed or restore window expired");
        } catch (DuplicateKeyException exception) {
            throw conflict("Destination already has an execution");
        }
        return get(patientId, id);
    }

    private Deleted deletion(UUID patientId, UUID id) {
        return jdbc.query(
                "select version,restore_token,restore_expires_at from meal_execution "
                        + "where id=? and patient_id=?",
                (rs, row) -> new Deleted(
                        id, rs.getLong(1), rs.getObject(2, UUID.class),
                        rs.getTimestamp(3) == null ? null : rs.getTimestamp(3).toInstant()
                ),
                id, patientId
        ).stream().findFirst()
                .orElseThrow(() -> new TrackingException(Kind.NOT_FOUND, "Execution was not found"));
    }

    private Context context(UUID patientId, LocalDate date, UUID plannedMealId) {
        if (plannedMealId == null) {
            return jdbc.query(
                    "select v.id from prescription p join prescription_version v on v.prescription_id=p.id "
                            + "where p.patient_id=? and p.status='ACTIVE' and p.planning_mode='FREE' "
                            + "and v.status='PUBLISHED' order by v.published_at desc limit 1",
                    (rs, row) -> new Context(null, rs.getObject(1, UUID.class), "FREE", null, null, null),
                    patientId
            ).stream().findFirst()
                    .orElseThrow(() -> invalid("A current published FREE prescription is required"));
        }
        var found = jdbc.query(
                "select p.id,p.prescription_version_id,p.planning_mode,m.meal_date,m.meal_time,m.meal_type::text "
                        + "from planned_meal m join weekly_plan p on p.id=m.weekly_plan_id "
                        + "where m.id=? and p.patient_id=?",
                (rs, row) -> new Context(
                        rs.getObject(1, UUID.class), rs.getObject(2, UUID.class),
                        rs.getString(3), rs.getObject(4, LocalDate.class),
                        rs.getObject(5, LocalTime.class), MealType.valueOf(rs.getString(6))
                ),
                plannedMealId, patientId
        ).stream().findFirst()
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
        for (var item : items) {
            if (item == null || item.foodId() == null || item.portionId() == null
                    || item.unit() == null || item.unit().isBlank() || item.quantity() == null
                    || item.quantity().signum() <= 0 || item.multiplier() == null
                    || item.multiplier().signum() <= 0)
                throw invalid("Each item needs food, portion, unit and positive quantity and multiplier");
            if (item.plannedItemId() != null && !seen.add(item.plannedItemId()))
                throw invalid("A planned item may appear only once");
            if (item.plannedItemId() == null && !"FREE".equals(context.mode()))
                throw invalid("This mode requires planned items");
            if (item.plannedItemId() != null) {
                var planned = jdbc.query(
                        "select food_id,prescribed_meal_item_id from planned_meal_item where id=? and planned_meal_id=?",
                        (rs, row) -> new UUID[] { rs.getObject(1, UUID.class), rs.getObject(2, UUID.class) },
                        item.plannedItemId(), mealId
                ).stream().findFirst()
                        .orElseThrow(() -> invalid("Planned item is unavailable"));
                if ("RESTRICTED".equals(context.mode()) && !planned[0].equals(item.foodId()))
                    throw invalid("Restricted executions must keep planned foods");
                if ("SUBSTITUTIONS_ONLY".equals(context.mode()) && !planned[0].equals(item.foodId())) {
                    if (item.allowedSubstitutionId() == null || planned[1] == null
                            || jdbc.queryForObject(
                                    "select count(*) from allowed_substitution where id=? and prescribed_meal_item_id=? and substitute_food_id=? "
                                            + "and (substitute_food_portion_id is null or substitute_food_portion_id=?)",
                                    Integer.class, item.allowedSubstitutionId(), planned[1], item.foodId(),
                                    item.portionId()
                            ) == 0)
                        throw invalid("Substitution is not authorized");
                } else if (item.allowedSubstitutionId() != null)
                    throw invalid("Substitution reference is not applicable");
            } else if (item.allowedSubstitutionId() != null)
                throw invalid("Substitution requires a planned item");
            portion(item);
        }
        if (!"FREE".equals(context.mode()) && !items.isEmpty()) {
            int count = jdbc.queryForObject(
                    "select count(*) from planned_meal_item where planned_meal_id=?",
                    Integer.class, mealId
            );
            if (seen.size() != count)
                throw invalid("Every planned item must be represented");
        }
    }

    private Portion portion(ItemInput item) {
        var portion = jdbc.query(
                "select p.quantity,p.unit,p.grams_equivalent from food_portion p "
                        + "join food f on f.id=p.food_id where f.id=? and p.id=? and f.active and p.active",
                (rs, row) -> new Portion(rs.getBigDecimal(1), rs.getString(2), rs.getBigDecimal(3)),
                item.foodId(), item.portionId()
        ).stream().findFirst()
                .orElseThrow(() -> invalid("Food or portion is missing or inactive"));
        if (portion.unit() != null && !portion.unit().equals(item.unit().trim()))
            throw invalid("Unit does not match the selected portion");
        return portion;
    }

    private void saveItems(UUID id, List<ItemInput> inputs) {
        int order = 0;
        for (var input : inputs) {
            var portion = portion(input);
            var factor = input.quantity().multiply(input.multiplier())
                    .divide(portion.quantity(), 12, RoundingMode.HALF_UP);
            var grams = portion.gramsEquivalent() == null ? null
                    : portion.gramsEquivalent().multiply(factor).setScale(4, RoundingMode.HALF_UP);
            var kcal = jdbc.query(
                    "select amount from food_portion_nutrient where food_portion_id=? "
                            + "and nutrient_code='energy_kcal' and amount is not null",
                    (rs, row) -> rs.getBigDecimal(1).multiply(factor)
                            .setScale(4, RoundingMode.HALF_UP),
                    input.portionId()
            ).stream()
                    .findFirst().orElse(null);
            var itemId = UUID.randomUUID();
            jdbc.update(
                    "insert into meal_execution_item(id,execution_id,item_order,planned_item_id,allowed_substitution_id,food_id,food_portion_id,quantity,multiplier,unit,grams,kcal) "
                            + "values (?,?,?,?,?,?,?,?,?,?,?,?)",
                    itemId, id, order++, input.plannedItemId(), input.allowedSubstitutionId(),
                    input.foodId(), input.portionId(), input.quantity(), input.multiplier(),
                    input.unit().trim(), grams, kcal
            );
            jdbc.update(
                    "insert into meal_execution_nutrient_snapshot(item_id,nutrient_code,amount,unit) "
                            + "select ?,n.nutrient_code,round(n.amount * ? / p.quantity,4),n.unit "
                            + "from food_portion_nutrient n join food_portion p on p.id=n.food_portion_id "
                            + "where n.food_portion_id=? and n.amount is not null",
                    itemId, input.quantity().multiply(input.multiplier()), input.portionId()
            );
        }
    }

    private Execution execution(ResultSet rs, UUID patientId) throws SQLException {
        UUID id = rs.getObject(1, UUID.class);
        var items = jdbc.query(
                "select id,food_id,food_portion_id,unit,quantity,multiplier,planned_item_id,allowed_substitution_id,grams,kcal "
                        + "from meal_execution_item where execution_id=? order by item_order",
                (ir, row) -> new Item(
                        ir.getObject(1, UUID.class), ir.getObject(2, UUID.class),
                        ir.getObject(3, UUID.class), ir.getString(4), ir.getBigDecimal(5),
                        ir.getBigDecimal(6), ir.getObject(7, UUID.class), ir.getObject(8, UUID.class),
                        ir.getBigDecimal(9), ir.getBigDecimal(10), itemNutrients(ir.getObject(1, UUID.class))
                ), id
        );
        var totals = new HashMap<String, NutrientTotal>();
        for (var item : items)
            item.nutrients().forEach(
                    (code, value) -> totals.merge(
                            code, value,
                            (a, b) -> new NutrientTotal(a.amount().add(b.amount()), a.unit())
                    )
            );
        return new Execution(
                id, rs.getLong(2), rs.getObject(3, LocalDate.class),
                rs.getObject(4, LocalTime.class), MealType.valueOf(rs.getString(5)),
                rs.getObject(6, UUID.class), rs.getObject(7, UUID.class),
                rs.getObject(8, UUID.class), rs.getString(9), items, totals,
                rs.getTimestamp(10).toInstant(), rs.getTimestamp(11).toInstant(),
                rs.getTimestamp(12) == null ? null : rs.getTimestamp(12).toInstant(),
                rs.getTimestamp(13) == null ? null : rs.getTimestamp(13).toInstant()
        );
    }

    private Map<String, NutrientTotal> itemNutrients(UUID itemId) {
        var values = new HashMap<String, NutrientTotal>();
        jdbc.query(
                "select nutrient_code,amount,unit from meal_execution_nutrient_snapshot where item_id=?",
                (org.springframework.jdbc.core.RowCallbackHandler) rs -> values.put(
                        rs.getString(1),
                        new NutrientTotal(rs.getBigDecimal(2), rs.getString(3))
                ), itemId
        );
        return values;
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
        var result = jdbc.query(
                "select command_hash,execution_id from meal_execution_command where patient_id=? and idempotency_key=?",
                (rs, row) -> new String[] { rs.getString(1), rs.getString(2) },
                patientId, key
        ).stream().findFirst();
        if (result.isEmpty())
            return null;
        if (!result.get()[0].equals(hash))
            throw conflict("Idempotency-Key was used with different content");
        return UUID.fromString(result.get()[1]);
    }

    private void saveCommand(UUID patientId, String key, String hash, UUID id, long version) {
        try {
            jdbc.update(
                    "insert into meal_execution_command(patient_id,idempotency_key,command_hash,execution_id,result_version) values (?,?,?,?,?)",
                    patientId, key, hash, id, version
            );
        } catch (DuplicateKeyException exception) {
            throw conflict("Idempotency-Key is already in use");
        }
    }

    private void saveResponse(UUID patientId, String key, Object result) {
        try {
            jdbc.update(
                    "update meal_execution_command set response_json=?::jsonb "
                            + "where patient_id=? and idempotency_key=?",
                    json.writeValueAsString(result), patientId, key
            );
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
        return saved == null ? deletion(patientId, id) : saved;
    }

    private <T> T response(UUID patientId, String key, Class<T> type) {
        String payload = jdbc.query(
                "select response_json::text from meal_execution_command "
                        + "where patient_id=? and idempotency_key=?",
                (rs, row) -> rs.getString(1), patientId, key
        ).stream().findFirst().orElse(null);
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
