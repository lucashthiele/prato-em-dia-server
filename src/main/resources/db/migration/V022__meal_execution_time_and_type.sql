ALTER TABLE meal_execution
    ADD COLUMN meal_time TIME,
    ADD COLUMN meal_type meal_type;

UPDATE meal_execution e
SET meal_time = COALESCE(m.meal_time, CAST(e.created_at AT TIME ZONE 'UTC' AS TIME)),
    meal_type = m.meal_type
FROM planned_meal m
WHERE e.planned_meal_id = m.id;

UPDATE meal_execution
SET meal_time = CAST(created_at AT TIME ZONE 'UTC' AS TIME),
    meal_type = CASE
        WHEN CAST(created_at AT TIME ZONE 'UTC' AS TIME) < TIME '10:00' THEN 'BREAKFAST'::meal_type
        WHEN CAST(created_at AT TIME ZONE 'UTC' AS TIME) < TIME '15:00' THEN 'LUNCH'::meal_type
        WHEN CAST(created_at AT TIME ZONE 'UTC' AS TIME) < TIME '18:00' THEN 'SNACK'::meal_type
        ELSE 'DINNER'::meal_type
    END
WHERE meal_type IS NULL;

ALTER TABLE meal_execution
    ALTER COLUMN meal_time SET NOT NULL,
    ALTER COLUMN meal_type SET NOT NULL;
