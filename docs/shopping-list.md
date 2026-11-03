# Lista de compras

Lista persistida por plano semanal (LUC-30), em `/api/v1/patients/me/weekly-plans/{planId}/shopping-list`.
Só o paciente dono do plano acessa (outro paciente ou plano inexistente ou excluído: `404`).

| Método | Resposta |
| --- | --- |
| `POST` | Cria a lista a partir das refeições planejadas do plano. `201` ao criar, `200` se já existe lista ativa (idempotente) |
| `GET` | A lista ativa, ou `404 SHOPPING_LIST_NOT_FOUND`. O cliente chama o `POST` ao receber `404` |

A resposta traz `id`, `weeklyPlanId`, `createdAt` e `items[]` com `foodId`, `foodName`, `portionId`,
`portionLabel`, `unit` e `quantity`. Os itens são os alimentos planejados somados por alimento,
porção e unidade, na ordem em que aparecem no plano. Não há decomposição de pratos em ingredientes
(o catálogo não modela receita). O "comprado" fica só no app, guardado por `foodId` + `portionId` +
`unit`.

## Quando a lista deixa de valer

Qualquer mudança nas refeições planejadas do plano (edição, troca de substituição, regeneração que
substitui dias, exclusão do plano) desativa a lista **na mesma transação**. Isso é feito por
triggers no banco (V041), em `planned_meal`, `planned_meal_item` e `weekly_plan.active`, para valer
para qualquer caminho de escrita sem acoplar o módulo de planejamento. A lista desativada fica como
histórico e o próximo `POST` cria outra. Confirmar o plano não desativa a lista.

O índice único parcial `ux_shopping_list_active_plan` (`weekly_plan_id` onde `active`) garante uma
lista ativa por plano. O `POST` trava a linha do plano até o fim da transação, então dois `POST`
simultâneos não duplicam.

## Atenção na integração com o planejamento

`PlanningQueryAdapter.plannedItems` lê todas as refeições do plano. Com a coluna
`planned_meal.superseded_at` (ADR 0002), a leitura deve excluir as refeições substituídas
(`superseded_at IS NULL`); o filtro entra na consulta `findAllOfPlan` de `PlannedMealItemJpaRepository`.
Um `UPDATE` que marca `superseded_at` já desativa a lista pelo trigger.
