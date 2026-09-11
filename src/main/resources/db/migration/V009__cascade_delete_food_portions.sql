ALTER TABLE food_portion
    DROP CONSTRAINT food_portion_food_id_fkey;

ALTER TABLE food_portion
    ADD CONSTRAINT fk_food_portion_food
        FOREIGN KEY (food_id)
        REFERENCES food (id)
        ON DELETE CASCADE;
