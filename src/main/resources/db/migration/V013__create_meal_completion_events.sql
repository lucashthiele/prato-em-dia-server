CREATE TYPE meal_completion_event_type AS ENUM ('COMPLETED', 'NOT_COMPLETED', 'SUBSTITUTED');

CREATE TABLE meal_completion_event
(
    sequence_id BIGINT GENERATED ALWAYS AS IDENTITY PRIMARY KEY,
    id UUID NOT NULL UNIQUE,
    patient_id UUID NOT NULL REFERENCES patient_profile (id),
    weekly_plan_id UUID NOT NULL REFERENCES weekly_plan (id),
    planned_meal_id UUID NOT NULL,
    event_type meal_completion_event_type NOT NULL,
    reason TEXT,
    occurred_at TIMESTAMPTZ NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    idempotency_key VARCHAR(255) NOT NULL,
    command_hash VARCHAR(64) NOT NULL,
    meal_snapshot JSONB NOT NULL,
    planned_items_snapshot JSONB NOT NULL,
    consumed_items_snapshot JSONB NOT NULL,
    CONSTRAINT ux_completion_patient_key UNIQUE (patient_id, idempotency_key)
);
CREATE INDEX ix_completion_patient_sequence ON meal_completion_event (patient_id, sequence_id DESC);
CREATE INDEX ix_completion_plan_meal_sequence ON meal_completion_event (weekly_plan_id, planned_meal_id, sequence_id DESC);

CREATE FUNCTION reject_meal_completion_event_mutation() RETURNS trigger AS $$
BEGIN
    RAISE EXCEPTION 'meal completion events are append-only';
END;
$$ LANGUAGE plpgsql;
CREATE TRIGGER meal_completion_event_append_only
    BEFORE UPDATE OR DELETE ON meal_completion_event
    FOR EACH ROW EXECUTE FUNCTION reject_meal_completion_event_mutation();
