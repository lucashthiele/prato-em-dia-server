# Trabalho manual: W2 (registro e leituras)

Passos que o código não resolve. Cada item é checável.

## Depois de integrar os branches (todos os tickets)

- [ ] Rodar `./mvnw test` no branch integrado. As migrations deste worker são V040 e V041.
- [ ] Confirmar que o catálogo de códigos de erro (LUC-50) lista os códigos novos: `SHOPPING_LIST_NOT_FOUND` (404), `SHOPPING_PLAN_NOT_FOUND` (404) e `SHOPPING_ACCESS_DENIED` (403). Se o teste do catálogo falhar, acrescentar as três linhas.
- [ ] Rodar `docker compose up -d`, subir o servidor com o perfil `dev-seed` e abrir `/swagger-ui` para ver as rotas novas (ver cada ticket abaixo).

## LUC-35: várias execuções por categoria e dia

- [ ] No app mobile (dispositivo ou emulador), registrar duas execuções no mesmo almoço do mesmo dia, com horários diferentes, e conferir que as duas aparecem na lista do dia. O servidor já aceita.
- [ ] Mover uma execução para um dia/refeição que já tem execução e conferir que não há mais erro `409`.
- [ ] Atualizar `mobile/prato_em_dia_mobile/docs/mudancas-de-servidor.md` e `portal-web/docs/mudancas-de-servidor.md` (fora deste repositório): criar e mover aceitam destino ocupado, e uma execução pode cobrir só parte dos itens planejados.

## LUC-36, LUC-41 e LUC-71: estado e permissão por categoria

- [ ] No app, trocar a lógica local de estado/bloqueio por `GET /api/v1/patients/me/daily-meal-categories?date=`: `state` (`NOT_LOGGED`, `PARTIAL`, `LOGGED`), `canRegister` e `blockedReason` (`NO_ALLOWED_FOODS`).
- [ ] Conferir em dispositivo, com uma prescrição `RESTRICTED` que tenha uma categoria sem itens, que o app mostra a categoria bloqueada, e com uma prescrição `FREE` que ela aceita registro.
- [ ] Produto: confirmar o texto da UI para `blockedReason = NO_ALLOWED_FOODS`. Dia sem plano em modo restrito, e dia sem prescrição publicada, também devolvem esse motivo.

## LUC-64: prescription-summary

- [ ] No portal, regenerar os tipos a partir de `/v3/api-docs` e trocar as chamadas `GET /foods/{id}` por `GET /nutritionists/me/patients/{patientId}/prescription-summary`.
- [ ] Produto: confirmar a exibição de `complete = false` (total incompleto, por item sem porção ou sem o nutriente) e que os totais são de um dia da prescrição, como decidido no roadmap.

## LUC-28: leituras do nutricionista

- [ ] No portal, usar `/nutritionists/me/patients/{patientId}/meal-executions?from=&to=` (máximo 92 dias por chamada; para períodos maiores, fazer mais de uma chamada), `daily-meal-categories`, `daily-nutrition-summary` e `calendar-periods`.
- [ ] Com o servidor local e uma conta de nutricionista do seed, abrir um paciente vinculado e conferir os dados; com um paciente de outro nutricionista, conferir o `403`.

## LUC-30: lista de compras

- [ ] **Depois de integrar o W1 (`planned_meal.superseded_at`, ADR 0002):** acrescentar o filtro de refeições não substituídas em `PlannedMealItemJpaRepository.findAllOfPlan` (ver `docs/shopping-list.md`) e rodar `ShoppingListIntegrationTest`. Sem isso a lista incluiria refeições substituídas por regeneração.
- [ ] No app, chamar `POST .../shopping-list` ao receber `404` do `GET`, e guardar o "comprado" por `foodId` + `portionId` + `unit`. Conferir em dispositivo que, ao editar uma refeição do plano, o `GET` volta `404` e o `POST` cria uma lista nova.
