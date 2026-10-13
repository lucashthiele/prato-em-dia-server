CREATE TABLE meal_execution (
    id UUID PRIMARY KEY,
    patient_id UUID NOT NULL REFERENCES patient_profile(id),
    execution_date DATE NOT NULL,
    planned_meal_id UUID REFERENCES planned_meal(id),
    weekly_plan_id UUID REFERENCES weekly_plan(id),
    prescription_version_id UUID NOT NULL REFERENCES prescription_version(id),
    mode planning_mode NOT NULL,
    version BIGINT NOT NULL DEFAULT 1,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deleted_at TIMESTAMPTZ,
    restore_token UUID,
    restore_expires_at TIMESTAMPTZ,
    CONSTRAINT ck_meal_execution_context CHECK (
        (planned_meal_id IS NULL AND weekly_plan_id IS NULL AND mode = 'FREE')
        OR (planned_meal_id IS NOT NULL AND weekly_plan_id IS NOT NULL)
    )
);
CREATE INDEX ix_meal_execution_patient_date ON meal_execution(patient_id, execution_date)
    WHERE deleted_at IS NULL;
CREATE UNIQUE INDEX ux_meal_execution_active_meal ON meal_execution(planned_meal_id)
    WHERE deleted_at IS NULL AND planned_meal_id IS NOT NULL;

CREATE TABLE meal_execution_item (
    id UUID PRIMARY KEY,
    execution_id UUID NOT NULL REFERENCES meal_execution(id) ON DELETE CASCADE,
    item_order INTEGER NOT NULL CHECK (item_order >= 0),
    planned_item_id UUID,
    allowed_substitution_id UUID,
    food_id UUID NOT NULL REFERENCES food(id),
    food_portion_id UUID NOT NULL REFERENCES food_portion(id),
    quantity NUMERIC(12,3) NOT NULL CHECK (quantity > 0),
    multiplier NUMERIC(12,3) NOT NULL CHECK (multiplier > 0),
    unit VARCHAR(32) NOT NULL,
    grams NUMERIC(16,4),
    kcal NUMERIC(16,4),
    CONSTRAINT ux_meal_execution_item_order UNIQUE (execution_id, item_order)
);
CREATE INDEX ix_meal_execution_item_execution ON meal_execution_item(execution_id);

CREATE TABLE meal_execution_nutrient_snapshot (
    item_id UUID NOT NULL REFERENCES meal_execution_item(id) ON DELETE CASCADE,
    nutrient_code VARCHAR(64) NOT NULL,
    amount NUMERIC(16,4) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    PRIMARY KEY (item_id, nutrient_code)
);

CREATE TABLE meal_execution_command (
    patient_id UUID NOT NULL REFERENCES patient_profile(id),
    idempotency_key VARCHAR(255) NOT NULL,
    command_hash VARCHAR(64) NOT NULL,
    execution_id UUID NOT NULL REFERENCES meal_execution(id),
    result_version BIGINT NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (patient_id, idempotency_key)
);
