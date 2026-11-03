-- LUC-30: lista de compras persistida por plano semanal.
CREATE TABLE shopping_list (
    id UUID PRIMARY KEY,
    weekly_plan_id UUID NOT NULL REFERENCES weekly_plan(id),
    patient_id UUID NOT NULL REFERENCES patient_profile(id),
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    deactivated_at TIMESTAMPTZ,
    CONSTRAINT ck_shopping_list_deactivation CHECK (active = (deactivated_at IS NULL))
);

-- No máximo uma lista ativa por plano; as desativadas ficam como histórico.
CREATE UNIQUE INDEX ux_shopping_list_active_plan ON shopping_list(weekly_plan_id) WHERE active;
CREATE INDEX ix_shopping_list_plan ON shopping_list(weekly_plan_id);

CREATE TABLE shopping_list_item (
    id UUID PRIMARY KEY,
    shopping_list_id UUID NOT NULL REFERENCES shopping_list(id) ON DELETE CASCADE,
    food_id UUID NOT NULL REFERENCES food(id),
    food_portion_id UUID REFERENCES food_portion(id),
    unit VARCHAR(32) NOT NULL,
    quantity NUMERIC(14,3) NOT NULL CHECK (quantity > 0),
    item_order INTEGER NOT NULL CHECK (item_order >= 0),
    CONSTRAINT ux_shopping_list_item_order UNIQUE (shopping_list_id, item_order)
);

CREATE UNIQUE INDEX ux_shopping_list_item_key ON shopping_list_item (
    shopping_list_id, food_id, COALESCE(food_portion_id, '00000000-0000-0000-0000-000000000000'::uuid), unit
);

-- Qualquer mudança nas refeições planejadas de um plano (edição, troca de substituição,
-- regeneração que substitui dias, exclusão) desativa a lista na mesma transação. Fica no banco
-- para valer para qualquer caminho de escrita, sem acoplar o módulo de planejamento a este.
CREATE FUNCTION deactivate_shopping_list(plan UUID) RETURNS void AS $$
    UPDATE shopping_list
    SET active = FALSE, deactivated_at = CURRENT_TIMESTAMP
    WHERE weekly_plan_id = plan AND active;
$$ LANGUAGE sql;

CREATE FUNCTION trg_deactivate_shopping_list_by_meal() RETURNS trigger AS $$
BEGIN
    PERFORM deactivate_shopping_list(COALESCE(NEW.weekly_plan_id, OLD.weekly_plan_id));
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_planned_meal_deactivates_shopping_list_ins_del
    AFTER INSERT OR DELETE ON planned_meal
    FOR EACH ROW EXECUTE FUNCTION trg_deactivate_shopping_list_by_meal();

CREATE TRIGGER trg_planned_meal_deactivates_shopping_list_upd
    AFTER UPDATE ON planned_meal
    FOR EACH ROW WHEN (OLD IS DISTINCT FROM NEW)
    EXECUTE FUNCTION trg_deactivate_shopping_list_by_meal();

CREATE FUNCTION trg_deactivate_shopping_list_by_item() RETURNS trigger AS $$
DECLARE
    meal UUID := COALESCE(NEW.planned_meal_id, OLD.planned_meal_id);
    plan UUID;
BEGIN
    SELECT weekly_plan_id INTO plan FROM planned_meal WHERE id = meal;
    IF plan IS NOT NULL THEN
        PERFORM deactivate_shopping_list(plan);
    END IF;
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_planned_meal_item_deactivates_shopping_list_ins_del
    AFTER INSERT OR DELETE ON planned_meal_item
    FOR EACH ROW EXECUTE FUNCTION trg_deactivate_shopping_list_by_item();

CREATE TRIGGER trg_planned_meal_item_deactivates_shopping_list_upd
    AFTER UPDATE ON planned_meal_item
    FOR EACH ROW WHEN (OLD IS DISTINCT FROM NEW)
    EXECUTE FUNCTION trg_deactivate_shopping_list_by_item();

CREATE FUNCTION trg_deactivate_shopping_list_by_plan() RETURNS trigger AS $$
BEGIN
    PERFORM deactivate_shopping_list(NEW.id);
    RETURN NULL;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER trg_weekly_plan_deactivates_shopping_list
    AFTER UPDATE OF active ON weekly_plan
    FOR EACH ROW WHEN (OLD.active AND NOT NEW.active)
    EXECUTE FUNCTION trg_deactivate_shopping_list_by_plan();
