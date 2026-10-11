# Registro de alimentação

Esta entrega não persiste rascunho remoto. A tela mantém qualquer edição não
enviada localmente e grava somente eventos imutáveis em
`meal_completion_event`.

## Contexto da tela

`GET /api/v1/planned-meals/{plannedMealId}/food-log` é exclusivo do paciente
dono do plano e devolve:

```json
{
  "plannedMealId": "uuid",
  "mode": "RESTRICTED|SUBSTITUTIONS_ONLY|FREE",
  "items": [{
    "plannedItemId": "uuid",
    "catalogFoodId": "uuid",
    "catalogPortionId": "uuid ou null",
    "unit": "g",
    "quantity": 100,
    "multiplier": 1,
    "canonicalConversion": { "grams": 100 },
    "kcalSnapshot": 52,
    "allowedSubstitutions": []
  }]
}
```

`mode` vem sempre do `WeeklyPlan`. `allowedSubstitutions` só traz opções em
`SUBSTITUTIONS_ONLY`; em `RESTRICTED` fica vazio e em `FREE` o cliente usa o
catálogo. O endpoint não duplica nome ou rótulos de
catálogo: o cliente resolve exibição por `GET /api/v1/foods/{foodId}`. Os
campos calculados só aparecem como valor quando a porção ativa do catálogo
permite calculá-los; não são substituídos por zero.

## Registro append-only

`POST /api/v1/planned-meals/{plannedMealId}/completion-events` exige
`Idempotency-Key` e aceita `occurredAt`, `reason` opcional e `consumedItems`.
Cada item tem `catalogFoodId`, `catalogPortionId` obrigatório, `unit`, `quantity`,
`multiplier`, `plannedItemId` opcional e `allowedSubstitutionId` opcional.
`allowedSubstitutionId` é aceito exclusivamente em `SUBSTITUTIONS_ONLY`.

O paciente nunca envia `mode`, `eventType`, conversão, kcal nem snapshots. O
servidor valida posse do plano, período, catálogo ativo, porção ativa pertencente
ao alimento e unidade da porção quando cadastrada, deriva os snapshots e calcula
o tipo: lista vazia é `NOT_COMPLETED`;
uma alternativa autorizada é `SUBSTITUTED`; as demais composições são
`COMPLETED`. Repetição com a mesma chave e payload devolve o mesmo evento;
payload diferente retorna `409`.

- Quando `consumedItems` não é vazia, ela contém exatamente uma linha por item
  planejado; `plannedItemId` duplicado ou ausente é inválido. Linhas adicionais
  sem `plannedItemId` existem apenas em `FREE`.
- `RESTRICTED`: cada item referencia um planejado e usa o mesmo alimento.
- `SUBSTITUTIONS_ONLY`: aceita o alimento planejado ou uma autorização válida
  para aquele item planejado. O snapshot consumido contém a alternativa, não
  o alimento original, e retém `allowedSubstitutionId` como referência técnica.
- `FREE`: aceita alimento ativo. Um alimento novo não referencia item planejado
  e exige porção e quantidade/multiplicador positivos. Remover o planejado é
  uma linha com seu `plannedItemId`, o alimento originalmente planejado e
  `quantity: 0`; ela pode indicar outra porção ativa desse mesmo alimento. Para
  troca livre, essa linha zero acompanha uma nova linha sem `plannedItemId`.

A unidade declarada é obrigatória. Quando a porção do catálogo tem unidade,
ela deve ser exatamente essa unidade; se o registro legado da porção não tem
unidade, o servidor não inventa uma unidade clínica e aceita a unidade
declarada sem compará-la.

O contexto GET preserva `catalogPortionId` opcional do planejamento legado,
mas uma linha enviada no POST nunca pode omiti-lo.

O planejamento e sua prescrição nunca são alterados por esse registro. Os
snapshots de refeição, planejado e consumido permanecem separados. Não há
edição ou exclusão de evento salvo nesta entrega.
