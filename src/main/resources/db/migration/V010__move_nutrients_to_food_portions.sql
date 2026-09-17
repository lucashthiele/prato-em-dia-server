ALTER TABLE food
    ADD COLUMN brand VARCHAR(180);

ALTER TABLE food_portion
    ALTER COLUMN unit DROP NOT NULL,
    ADD COLUMN source_type VARCHAR(32),
    ADD COLUMN raw_label   VARCHAR(100),
    ADD CONSTRAINT ck_food_portion_source_type CHECK (
        source_type IS NULL OR source_type IN ('STANDARD', 'HOUSEHOLD')
        );

-- Preserve nutrients imported by the previous TACO importer by attaching them
-- to a generated standard 100 g portion before removing the old association.
INSERT INTO food_portion (id, food_id, label, quantity, unit, grams_equivalent, source_type, raw_label)
SELECT (
           SUBSTRING(MD5(food_id::text || ':STANDARD:100:g'), 1, 8) || '-' ||
           SUBSTRING(MD5(food_id::text || ':STANDARD:100:g'), 9, 4) || '-' ||
           SUBSTRING(MD5(food_id::text || ':STANDARD:100:g'), 13, 4) || '-' ||
           SUBSTRING(MD5(food_id::text || ':STANDARD:100:g'), 17, 4) || '-' ||
           SUBSTRING(MD5(food_id::text || ':STANDARD:100:g'), 21, 12)
           )::uuid,
       food_id,
       '100 g',
       100,
       'g',
       100,
       'STANDARD',
       'Valor por 100g'
FROM (SELECT DISTINCT food_id FROM food_nutrient) existing
WHERE NOT EXISTS (SELECT 1
                  FROM food_portion portion
                  WHERE portion.food_id = existing.food_id
                    AND portion.source_type = 'STANDARD');

CREATE TABLE food_portion_nutrient
(
    id              UUID PRIMARY KEY,
    food_portion_id UUID        NOT NULL REFERENCES food_portion (id) ON DELETE CASCADE,
    nutrient_code   VARCHAR(64) NOT NULL,
    amount          NUMERIC,
    unit            VARCHAR(16) NOT NULL,
    value_status    VARCHAR(32) NOT NULL,
    raw_value       VARCHAR(64),
    created_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_food_portion_nutrient_portion_code
        UNIQUE (food_portion_id, nutrient_code),
    CONSTRAINT ck_food_portion_nutrient_status CHECK (
        value_status IN (
                         'VALUE', 'TRACE', 'NOT_AVAILABLE', 'NOT_APPLICABLE', 'NOT_ANALYZED'
            )
        ),
    CONSTRAINT ck_food_portion_nutrient_amount_status CHECK (
        (value_status = 'VALUE' AND amount IS NOT NULL)
            OR
        (value_status <> 'VALUE' AND amount IS NULL)
        ),
    CONSTRAINT ck_food_portion_nutrient_amount_non_negative CHECK (
        amount IS NULL OR amount >= 0
        )
);

INSERT INTO food_portion_nutrient (id, food_portion_id, nutrient_code, amount, unit, value_status, raw_value,
                                   created_at, updated_at)
SELECT nutrient.id,
       portion.id,
       nutrient.nutrient_code,
       nutrient.amount,
       nutrient.unit,
       nutrient.value_status,
       nutrient.raw_value,
       nutrient.created_at,
       nutrient.updated_at
FROM food_nutrient nutrient
         JOIN food_portion portion
              ON portion.food_id = nutrient.food_id
                  AND portion.source_type = 'STANDARD';

DROP TABLE food_nutrient;

CREATE INDEX ix_food_portion_nutrient_portion
    ON food_portion_nutrient (food_portion_id);
CREATE INDEX ix_food_portion_nutrient_code
    ON food_portion_nutrient (nutrient_code);

ALTER TABLE catalog_import_run
    RENAME COLUMN file_name TO source_location;
ALTER TABLE catalog_import_run
    RENAME COLUMN file_checksum TO source_checksum;
