CREATE TABLE planned_meal_item_nutrient_snapshot (
    planned_item_id UUID NOT NULL REFERENCES planned_meal_item(id) ON DELETE CASCADE,
    nutrient_code VARCHAR(64) NOT NULL,
    amount NUMERIC(16,4) NOT NULL,
    unit VARCHAR(16) NOT NULL,
    PRIMARY KEY (planned_item_id, nutrient_code)
);

CREATE FUNCTION snapshot_planned_meal_item_nutrients() RETURNS trigger AS $$
BEGIN
    DELETE FROM planned_meal_item_nutrient_snapshot WHERE planned_item_id = NEW.id;
    IF NEW.food_portion_id IS NOT NULL THEN
        INSERT INTO planned_meal_item_nutrient_snapshot(planned_item_id,nutrient_code,amount,unit)
        SELECT NEW.id,n.nutrient_code,
               ROUND(n.amount * NEW.quantity / p.quantity, 4),n.unit
        FROM food_portion p
        JOIN food_portion_nutrient n ON n.food_portion_id=p.id
        WHERE p.id=NEW.food_portion_id AND n.amount IS NOT NULL;
    END IF;
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_snapshot_planned_meal_item_nutrients
    AFTER INSERT OR UPDATE OF food_id,food_portion_id,quantity,unit
    ON planned_meal_item FOR EACH ROW
    EXECUTE FUNCTION snapshot_planned_meal_item_nutrients();

INSERT INTO planned_meal_item_nutrient_snapshot(planned_item_id,nutrient_code,amount,unit)
SELECT i.id,n.nutrient_code,ROUND(n.amount * i.quantity / p.quantity,4),n.unit
FROM planned_meal_item i
JOIN food_portion p ON p.id=i.food_portion_id
JOIN food_portion_nutrient n ON n.food_portion_id=p.id
WHERE n.amount IS NOT NULL;
