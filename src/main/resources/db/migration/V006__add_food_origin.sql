CREATE TYPE food_origin AS ENUM (
    'ADMIN',
    'IMPORTED',
    'NUTRITIONIST',
    'PATIENT'
    );

ALTER TABLE food
    ADD COLUMN origin food_origin NOT NULL DEFAULT 'IMPORTED';

CREATE INDEX ix_food_origin ON food (origin);
