CREATE TYPE planning_mode AS ENUM ('FREE', 'SUBSTITUTIONS_ONLY', 'RESTRICTED');
CREATE TYPE meal_type AS ENUM ('BREAKFAST', 'LUNCH', 'SNACK', 'DINNER');
CREATE TYPE weekly_plan_status AS ENUM ('PENDING', 'CONFIRMED');

ALTER TABLE prescription
    ADD COLUMN planning_mode planning_mode NOT NULL DEFAULT 'RESTRICTED';
ALTER TABLE prescribed_meal
    ADD COLUMN meal_type meal_type NOT NULL;
ALTER TABLE prescribed_meal
    ADD CONSTRAINT ux_prescribed_meal_type UNIQUE (prescription_version_id, meal_type);

CREATE TABLE weekly_planning_preference
(
    patient_id  UUID PRIMARY KEY REFERENCES patient_profile (id) ON DELETE CASCADE,
    day_of_week INTEGER     NOT NULL CHECK (day_of_week BETWEEN 1 AND 7),
    enabled     BOOLEAN     NOT NULL DEFAULT TRUE,
    created_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE weekly_plan
(
    id                      UUID PRIMARY KEY,
    patient_id              UUID               NOT NULL REFERENCES patient_profile (id),
    prescription_version_id UUID               NOT NULL REFERENCES prescription_version (id),
    planning_mode           planning_mode      NOT NULL,
    period_start            DATE               NOT NULL,
    period_end              DATE               NOT NULL,
    confirmation_deadline   TIMESTAMPTZ        NOT NULL,
    status                  weekly_plan_status NOT NULL DEFAULT 'PENDING',
    confirmed_at            TIMESTAMPTZ,
    created_at              TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at              TIMESTAMPTZ        NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                 BIGINT             NOT NULL DEFAULT 0,
    CONSTRAINT ck_weekly_plan_period CHECK (period_end = period_start + 6),
    CONSTRAINT ck_weekly_plan_confirmation CHECK (
        (status = 'PENDING' AND confirmed_at IS NULL)
            OR (status = 'CONFIRMED' AND confirmed_at IS NOT NULL)
        ),
    CONSTRAINT ux_weekly_plan_patient_period UNIQUE (patient_id, period_start, period_end)
);
CREATE INDEX ix_weekly_plan_patient_start ON weekly_plan (patient_id, period_start DESC);
CREATE INDEX ix_weekly_plan_pending_deadline ON weekly_plan (status, confirmation_deadline);

CREATE TABLE planned_meal
(
    id                 UUID PRIMARY KEY,
    weekly_plan_id     UUID         NOT NULL REFERENCES weekly_plan (id) ON DELETE CASCADE,
    prescribed_meal_id UUID REFERENCES prescribed_meal (id),
    meal_date          DATE         NOT NULL,
    meal_time          TIME,
    meal_type          meal_type    NOT NULL,
    name               VARCHAR(120) NOT NULL,
    meal_order         INTEGER      NOT NULL CHECK (meal_order >= 0),
    CONSTRAINT ux_planned_meal_order UNIQUE (weekly_plan_id, meal_date, meal_order),
    CONSTRAINT ux_planned_meal_type UNIQUE (weekly_plan_id, meal_date, meal_type)
);
CREATE INDEX ix_planned_meal_plan ON planned_meal (weekly_plan_id);

CREATE TABLE planned_meal_item
(
    id                      UUID PRIMARY KEY,
    planned_meal_id         UUID           NOT NULL REFERENCES planned_meal (id) ON DELETE CASCADE,
    prescribed_meal_item_id UUID REFERENCES prescribed_meal_item (id),
    allowed_substitution_id UUID REFERENCES allowed_substitution (id),
    food_id                 UUID           NOT NULL REFERENCES food (id),
    food_portion_id         UUID REFERENCES food_portion (id),
    quantity                NUMERIC(12, 3) NOT NULL CHECK (quantity > 0),
    unit                    VARCHAR(32)    NOT NULL,
    item_order              INTEGER        NOT NULL CHECK (item_order >= 0),
    CONSTRAINT ux_planned_item_order UNIQUE (planned_meal_id, item_order)
);
CREATE INDEX ix_planned_item_meal ON planned_meal_item (planned_meal_id);
CREATE INDEX ix_planned_item_food ON planned_meal_item (food_id);
