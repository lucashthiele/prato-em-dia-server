-- TBCA publishes food names up to 239 characters (e.g. preparations with their full ingredient list).
-- Imported names are kept as published, so the catalog stores up to 255. Manual creation keeps its 180 limit.
ALTER TABLE food
    ALTER COLUMN name TYPE VARCHAR(255),
    ALTER COLUMN normalized_name TYPE VARCHAR(255);
