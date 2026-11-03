-- LUC-39: o período do plano deixa de ser fixo em 7 dias (regeneração cobre só parte do período).
ALTER TABLE weekly_plan
    DROP CONSTRAINT ck_weekly_plan_period;
ALTER TABLE weekly_plan
    ADD CONSTRAINT ck_weekly_plan_period CHECK (period_end >= period_start);
