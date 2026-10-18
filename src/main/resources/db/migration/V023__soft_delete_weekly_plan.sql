ALTER TABLE weekly_plan
    ADD COLUMN active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE weekly_plan
    DROP CONSTRAINT ux_weekly_plan_patient_period;
CREATE UNIQUE INDEX ux_weekly_plan_patient_period_active
    ON weekly_plan (patient_id, period_start, period_end)
    WHERE active;
