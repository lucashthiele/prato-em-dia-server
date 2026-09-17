ALTER TABLE food_nutrient
    DROP CONSTRAINT food_nutrient_food_id_fkey;

ALTER TABLE food_nutrient
    ADD CONSTRAINT fk_food_nutrient_food
        FOREIGN KEY (food_id)
            REFERENCES food (id)
            ON DELETE CASCADE;
