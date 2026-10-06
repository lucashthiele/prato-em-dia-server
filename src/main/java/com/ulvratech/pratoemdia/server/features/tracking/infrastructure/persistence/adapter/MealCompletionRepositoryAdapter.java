package com.ulvratech.pratoemdia.server.features.tracking.infrastructure.persistence.adapter;

import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent;
import com.ulvratech.pratoemdia.server.features.tracking.domain.entity.MealCompletionEvent.Type;
import com.ulvratech.pratoemdia.server.features.tracking.domain.port.out.MealCompletionRepository;
import com.ulvratech.pratoemdia.server.shared.pagination.domain.PageResult;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Timestamp;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.stereotype.Repository;

@Repository
public class MealCompletionRepositoryAdapter implements MealCompletionRepository {
    private static final String COLUMNS = "sequence_id, id, patient_id, weekly_plan_id, planned_meal_id, "
            + "event_type, reason, occurred_at, created_at, idempotency_key, command_hash, "
            + "meal_snapshot::text AS meal_snapshot, planned_items_snapshot::text AS planned_items_snapshot, "
            + "consumed_items_snapshot::text AS consumed_items_snapshot";
    private final JdbcTemplate jdbc;

    public MealCompletionRepositoryAdapter(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static final RowMapper<MealCompletionEvent> MAPPER = (rs, row) -> new MealCompletionEvent(
            rs.getLong("sequence_id"), uuid(rs, "id"), uuid(rs, "patient_id"),
            uuid(rs, "weekly_plan_id"), uuid(rs, "planned_meal_id"),
            Type.valueOf(rs.getString("event_type")), rs.getString("reason"),
            rs.getTimestamp("occurred_at").toInstant(), rs.getTimestamp("created_at").toInstant(),
            rs.getString("idempotency_key"), rs.getString("command_hash"),
            rs.getString("meal_snapshot"), rs.getString("planned_items_snapshot"),
            rs.getString("consumed_items_snapshot")
    );

    private static UUID uuid(ResultSet rs, String name) throws SQLException {
        return rs.getObject(name, UUID.class);
    }

    @Override
    public Optional<MealCompletionEvent> byKey(UUID patientId, String key) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM meal_completion_event WHERE patient_id=? AND idempotency_key=?",
                MAPPER, patientId, key
        ).stream().findFirst();
    }

    @Override
    public Optional<UUID> patientForMeal(UUID mealId) {
        return jdbc.query(
                "SELECT patient_id FROM meal_completion_event WHERE planned_meal_id=? "
                        + "ORDER BY sequence_id DESC LIMIT 1",
                (rs, row) -> rs.getObject(1, UUID.class),
                mealId
        ).stream().findFirst();
    }

    @Override
    public MealCompletionEvent append(MealCompletionEvent e) {
        jdbc.update(
                """
                        INSERT INTO meal_completion_event
                        (id, patient_id, weekly_plan_id, planned_meal_id, event_type, reason, occurred_at,
                         idempotency_key, command_hash, meal_snapshot, planned_items_snapshot, consumed_items_snapshot)
                        VALUES (?, ?, ?, ?, CAST(? AS meal_completion_event_type), ?, ?, ?, ?,
                                CAST(? AS jsonb), CAST(? AS jsonb), CAST(? AS jsonb))
                        ON CONFLICT (patient_id, idempotency_key) DO NOTHING
                        """, e.id(), e.patientId(), e.weeklyPlanId(), e.plannedMealId(), e.eventType().name(),
                e.reason(), Timestamp.from(e.occurredAt()), e.idempotencyKey(), e.commandHash(),
                e.mealSnapshot(), e.plannedItemsSnapshot(), e.consumedItemsSnapshot()
        );
        return byKey(e.patientId(), e.idempotencyKey()).orElseThrow();
    }

    @Override
    public PageResult<MealCompletionEvent> forMeal(UUID mealId, int page, int size) {
        long count = jdbc.queryForObject(
                "SELECT count(*) FROM meal_completion_event WHERE planned_meal_id=?",
                Long.class, mealId
        );
        var rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM meal_completion_event WHERE planned_meal_id=? "
                        + "ORDER BY sequence_id DESC LIMIT ? OFFSET ?",
                MAPPER, mealId, size, (long) page * size
        );
        return result(rows, page, size, count);
    }

    @Override
    public PageResult<MealCompletionEvent> forPatient(UUID patientId, int page, int size) {
        long count = jdbc.queryForObject(
                "SELECT count(*) FROM meal_completion_event WHERE patient_id=?",
                Long.class, patientId
        );
        var rows = jdbc.query(
                "SELECT " + COLUMNS + " FROM meal_completion_event WHERE patient_id=? "
                        + "ORDER BY sequence_id DESC LIMIT ? OFFSET ?",
                MAPPER, patientId, size, (long) page * size
        );
        return result(rows, page, size, count);
    }

    @Override
    public List<MealCompletionEvent> forPlan(UUID planId) {
        return jdbc.query(
                "SELECT " + COLUMNS + " FROM meal_completion_event WHERE weekly_plan_id=? "
                        + "ORDER BY sequence_id ASC",
                MAPPER, planId
        );
    }

    private PageResult<MealCompletionEvent> result(
            List<MealCompletionEvent> rows, int page,
            int size, long count
    ) {
        return new PageResult<>(rows, page, size, count, (int) ((count + size - 1) / size));
    }
}
