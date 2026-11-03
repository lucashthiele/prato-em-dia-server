-- LUC-35: uma categoria pode ter várias execuções no mesmo dia e na mesma refeição planejada.
-- A cobertura dos itens planejados passa a ser a união das execuções ativas.
DROP INDEX ux_meal_execution_active_meal;
