CREATE EXTENSION IF NOT EXISTS pg_trgm;

UPDATE food
SET normalized_name = NULLIF(
        btrim(regexp_replace(lower(unaccent(name)), '[^a-z0-9]+', ' ', 'g')), ''
    )
WHERE normalized_name IS NULL;

CREATE INDEX ix_food_normalized_name_trgm
    ON food USING gin (normalized_name gin_trgm_ops)
    WHERE active = true;
