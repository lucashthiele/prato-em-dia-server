-- LUC-38 e LUC-29 (ADR 0002): a unidade substituída por uma regeneração é o dia.
-- O banco garante que um paciente não tem duas refeições ativas para a mesma categoria e dia.

-- O período exato deixa de ser único: a regeneração sempre sobrepõe dias de outro plano.
DROP INDEX ux_weekly_plan_patient_period_active;

-- Alvo da chave estrangeira composta, para o patient_id copiado não divergir do dono do plano.
ALTER TABLE weekly_plan
    ADD CONSTRAINT ux_weekly_plan_id_patient UNIQUE (id, patient_id);

ALTER TABLE planned_meal
    ADD COLUMN patient_id     UUID,
    ADD COLUMN superseded_at  TIMESTAMPTZ;

UPDATE planned_meal meal
SET patient_id = plan.patient_id
FROM weekly_plan plan
WHERE plan.id = meal.weekly_plan_id;

ALTER TABLE planned_meal
    ALTER COLUMN patient_id SET NOT NULL;

ALTER TABLE planned_meal
    ADD CONSTRAINT fk_planned_meal_plan_patient
        FOREIGN KEY (weekly_plan_id, patient_id)
            REFERENCES weekly_plan (id, patient_id) ON DELETE CASCADE;

-- Refeições de planos excluídos (exclusão lógica) viram histórico.
UPDATE planned_meal meal
SET superseded_at = CURRENT_TIMESTAMP
FROM weekly_plan plan
WHERE plan.id = meal.weekly_plan_id
  AND NOT plan.active;

-- Duplicatas já existentes (o bug do LUC-29): vale a refeição do plano criado por último.
UPDATE planned_meal meal
SET superseded_at = CURRENT_TIMESTAMP
FROM (SELECT m.id,
             ROW_NUMBER() OVER (PARTITION BY m.patient_id, m.meal_date, m.meal_type
                 ORDER BY p.created_at DESC, p.id DESC) AS position
      FROM planned_meal m
               JOIN weekly_plan p ON p.id = m.weekly_plan_id
      WHERE m.superseded_at IS NULL) ranked
WHERE ranked.id = meal.id
  AND ranked.position > 1;

CREATE UNIQUE INDEX ux_planned_meal_active_slot
    ON planned_meal (patient_id, meal_date, meal_type)
    WHERE superseded_at IS NULL;
