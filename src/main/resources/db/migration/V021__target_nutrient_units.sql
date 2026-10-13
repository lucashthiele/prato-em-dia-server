CREATE FUNCTION validate_target_nutrient_unit() RETURNS trigger AS $$
DECLARE
    catalog_unit VARCHAR(16);
BEGIN
    SELECT unit INTO catalog_unit FROM food_portion_nutrient
    WHERE nutrient_code = NEW.nutrient_code LIMIT 1;
    IF catalog_unit IS NOT NULL AND catalog_unit <> NEW.unit THEN
        RAISE EXCEPTION 'target nutrient % must use catalog unit %',
            NEW.nutrient_code, catalog_unit USING ERRCODE = '23514';
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_validate_target_nutrient_unit
    BEFORE INSERT OR UPDATE OF nutrient_code,unit ON prescription_nutrition_target
    FOR EACH ROW EXECUTE FUNCTION validate_target_nutrient_unit();
