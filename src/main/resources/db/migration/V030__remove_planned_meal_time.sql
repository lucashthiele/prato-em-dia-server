-- LUC-40: as quatro categorias não têm horário planejado; o horário vem só da execução.
-- O suggested_time da prescrição permanece em prescribed_meal.
ALTER TABLE planned_meal
    DROP COLUMN meal_time;
