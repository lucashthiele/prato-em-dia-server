CREATE TABLE food
(
    id                     UUID PRIMARY KEY,
    name                   VARCHAR(180) NOT NULL,
    category               VARCHAR(100),
    default_unit           VARCHAR(32)  NOT NULL,
    notes                  TEXT,
    preparation_method     TEXT,
    active                 BOOLEAN      NOT NULL DEFAULT TRUE,
    created_by_user_id     UUID         NOT NULL REFERENCES user_account (id),
    created_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at             TIMESTAMPTZ  NOT NULL DEFAULT CURRENT_TIMESTAMP,
    version                BIGINT       NOT NULL DEFAULT 0
);

CREATE INDEX ix_food_lower_name ON food (LOWER(name));
CREATE INDEX ix_food_category ON food (category);
CREATE INDEX ix_food_created_by_user ON food (created_by_user_id);

CREATE TABLE food_portion
(
    id               UUID PRIMARY KEY,
    food_id          UUID           NOT NULL REFERENCES food (id),
    label            VARCHAR(100)   NOT NULL,
    quantity         NUMERIC(12, 3) NOT NULL,
    unit             VARCHAR(32)    NOT NULL,
    grams_equivalent NUMERIC(12, 3),
    active           BOOLEAN        NOT NULL DEFAULT TRUE,
    created_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at       TIMESTAMPTZ    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_food_portion_quantity CHECK (quantity > 0),
    CONSTRAINT ck_food_portion_grams_equivalent CHECK (
        grams_equivalent IS NULL OR grams_equivalent > 0
    )
);

CREATE INDEX ix_food_portion_food ON food_portion (food_id);
