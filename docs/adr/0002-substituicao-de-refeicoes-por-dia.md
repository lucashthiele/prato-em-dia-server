# Regeneração substitui refeições planejadas por dia, com unicidade no banco

Status: aceita (2026-11-02)

Planos semanais passam a ter períodos variáveis (geração às 08:00 BRT) e a regeneração manual cobre só parte do período. Como `planned_meal` é por dia e a unicidade valia apenas dentro de um plano, dois planos ativos podiam ter, cada um, o seu almoço no mesmo dia, e o app mostrava refeições duplicadas (LUC-29). Decidimos que a unidade substituída por uma regeneração é o **dia**, e que o banco garante que não existem duas refeições ativas para a mesma categoria e dia.

## Decisões

- `planned_meal` ganha `patient_id` e `superseded_at`.
- Índice único parcial `(patient_id, meal_date, meal_type) WHERE superseded_at IS NULL`.
- Chave estrangeira composta `planned_meal(weekly_plan_id, patient_id)` para `weekly_plan(id, patient_id)`, para que o `patient_id` copiado não possa divergir do dono do plano.
- Regenerar marca como substituídas só as refeições dos dias que a nova geração cobre e cria o novo plano. Os dias não cobertos continuam ativos no plano antigo. As refeições substituídas e as execuções ligadas a elas ficam como histórico e não contam para o plano ativo.
- Gerações simultâneas (as três tentativas do scheduler, regeneração manual) disputam o índice. Quem perde recebe um `409` com código próprio, ou o plano já gerado, nunca uma duplicata.

## Alternativas descartadas

- **Substituir o plano inteiro:** perde o contexto dos dias não regenerados e contradiz o plano consolidado ("novas instâncias para os dias sobrepostos").
- **Proibir sobreposição por exclusão de período em `weekly_plan`:** impede a regeneração, que sempre sobrepõe.

## Consequências

- Uma migration com colunas novas e backfill de `patient_id`. Barata agora, antes do go-live.
- `ck_weekly_plan_period` (período fixo de 7 dias) e o índice único por período exato deixam de valer.
- Um plano sem nenhuma refeição ativa vira histórico.
