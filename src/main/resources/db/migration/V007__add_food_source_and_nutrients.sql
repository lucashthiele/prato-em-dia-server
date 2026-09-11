ALTER TABLE food
    ALTER COLUMN created_by_user_id DROP NOT NULL,
    ADD COLUMN source VARCHAR(32),
    ADD COLUMN source_external_id VARCHAR(64),
    ADD COLUMN source_description VARCHAR(255),
    ADD COLUMN source_version VARCHAR(100),
    ADD COLUMN normalized_name VARCHAR(180);

ALTER TABLE food
    ADD CONSTRAINT uq_food_source_external_id UNIQUE (source, source_external_id),
    ADD CONSTRAINT ck_food_source_identity CHECK (
        (source IS NULL AND source_external_id IS NULL AND source_description IS NULL)
        OR
        (source IS NOT NULL AND source_external_id IS NOT NULL AND source_description IS NOT NULL)
    ),
    ADD CONSTRAINT ck_food_creator_or_source CHECK (
        created_by_user_id IS NOT NULL OR source IS NOT NULL
    );

CREATE INDEX ix_food_normalized_name ON food (normalized_name);

CREATE TABLE food_nutrient
(
    id             UUID PRIMARY KEY,
    food_id        UUID        NOT NULL REFERENCES food (id) ON DELETE CASCADE,
    nutrient_code  VARCHAR(64) NOT NULL,
    amount         NUMERIC,
    unit           VARCHAR(16) NOT NULL,
    value_status   VARCHAR(32) NOT NULL,
    raw_value      VARCHAR(64),
    created_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at     TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_food_nutrient_food_code UNIQUE (food_id, nutrient_code),
    CONSTRAINT ck_food_nutrient_status CHECK (
        value_status IN ('VALUE', 'TRACE', 'NOT_AVAILABLE', 'NOT_APPLICABLE')
    ),
    CONSTRAINT ck_food_nutrient_amount_status CHECK (
        (value_status = 'VALUE' AND amount IS NOT NULL)
        OR
        (value_status <> 'VALUE' AND amount IS NULL)
    ),
    CONSTRAINT ck_food_nutrient_amount_non_negative CHECK (
        amount IS NULL OR amount >= 0
    )
);

CREATE INDEX ix_food_nutrient_food ON food_nutrient (food_id);
CREATE INDEX ix_food_nutrient_code ON food_nutrient (nutrient_code);

CREATE TABLE catalog_import_run
(
    id              UUID PRIMARY KEY,
    source          VARCHAR(32)  NOT NULL,
    source_version  VARCHAR(100),
    file_name       VARCHAR(255) NOT NULL,
    file_checksum   VARCHAR(64)  NOT NULL,
    dry_run         BOOLEAN      NOT NULL,
    started_at      TIMESTAMPTZ  NOT NULL,
    finished_at     TIMESTAMPTZ,
    status          VARCHAR(32)  NOT NULL,
    rows_read       INTEGER      NOT NULL DEFAULT 0,
    rows_inserted   INTEGER      NOT NULL DEFAULT 0,
    rows_updated    INTEGER      NOT NULL DEFAULT 0,
    rows_skipped    INTEGER      NOT NULL DEFAULT 0,
    rows_failed     INTEGER      NOT NULL DEFAULT 0
);

CREATE INDEX ix_catalog_import_run_source_started
    ON catalog_import_run (source, started_at DESC);
