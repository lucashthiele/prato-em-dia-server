CREATE FUNCTION enforce_nutrient_unit() RETURNS trigger AS $$
DECLARE
    known_unit VARCHAR(16);
BEGIN
    PERFORM pg_advisory_xact_lock(hashtext(NEW.nutrient_code));
    SELECT unit INTO known_unit FROM food_portion_nutrient
    WHERE nutrient_code = NEW.nutrient_code AND id <> NEW.id LIMIT 1;
    IF known_unit IS NOT NULL AND known_unit <> NEW.unit THEN
        RAISE EXCEPTION 'nutrient % must use unit %, got %',
            NEW.nutrient_code, known_unit, NEW.unit USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_enforce_nutrient_unit
    BEFORE INSERT OR UPDATE OF nutrient_code,unit ON food_portion_nutrient
    FOR EACH ROW EXECUTE FUNCTION enforce_nutrient_unit();
