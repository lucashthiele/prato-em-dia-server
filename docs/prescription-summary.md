# Resumo da prescrição para o portal

`GET /api/v1/nutritionists/me/patients/{patientId}/prescription-summary` (LUC-64). Somente leitura.
A autorização é a mesma de `GET .../prescriptions`: papel `NUTRITIONIST` e vínculo ativo com o
paciente (`403 PRESCRIPTION_ACCESS_DENIED` caso contrário). Só aparecem prescrições do
nutricionista logado.

## Resposta

- `prescriptionId` e `planningMode`: nulos quando o paciente não tem prescrição (200, com
  `current` e `draft` nulos e `history` vazio).
- `current`: versão `PUBLISHED`; `draft`: rascunho `DRAFT` aberto. Ambos opcionais.
- `history`: metadados de todas as versões (`id`, `number`, `status`, `title`, `createdAt`,
  `publishedAt`, `archivedAt`), da mais nova para a mais antiga, sem refeições. O detalhe de uma
  versão antiga continua em `GET /prescription-versions/{id}`.
- `VersionSummary`: `id`, `number`, `status`, `title`, `notes`, `createdAt`, `publishedAt`,
  `totals`, `targets[]` e `meals[]`.
- Refeição: `id`, `name`, `mealType`, `order`, `suggestedTime`, `notes`, `totals`, `items[]`.
- Item: `id`, `foodId`, `foodName`, `quantity`, `unit`, `portionLabel`, `notes`, `nutrients`,
  `substitutions[]` (`id`, `foodId`, `foodName`, `quantity`, `unit`, `portionLabel`, `notes`).

## Macros

`Macros` tem `energyKcal`, `protein`, `carbohydrate`, `lipids` (arredondados em duas casas) e
`complete`. A conta é `nutriente × quantity / portion.quantity`, comportamento de `FoodPortion` no
catálogo, o mesmo usado pelo diário alimentar.

- Item sem `foodPortionId`, ou porção sem o nutriente: o macro do item é `null` e
  `complete = false`.
- Totais de refeição e de versão somam os itens principais (substituições são alternativas e não
  entram) e são totais de **um dia** da prescrição. Um item incompleto contribui só com os macros
  conhecidos e marca `complete = false`, para o portal não exibir um total falso.
- `foodName` e `portionLabel` vêm de duas consultas em lote ao catálogo (sem N+1), inclusive para
  alimentos ou porções já desativados.
