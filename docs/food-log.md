# Diário alimentar

O registro salvo é uma `MealExecution` mutável. Cada execução pertence ao paciente,
tem uma data e pode apontar para uma refeição planejada. `completion-events` não
faz parte desta API.

## Contexto e calendário

- `GET /api/v1/planned-meals/{plannedMealId}/food-log` devolve modo, itens
  planejados, conversão, kcal disponíveis e substituições autorizadas. Apenas o
  paciente dono do plano pode consultá-lo.
- `GET /api/v1/patients/me/calendar-periods` devolve `plans` com início/fim,
  `prescriptions` com o intervalo entre publicação e arquivamento, e
  `freeExecutionDates` com datas isoladas de execuções livres ativas.
- `GET /api/v1/patients/me/daily-nutrition-summary?date=YYYY-MM-DD` devolve
  `date`, `planId` e `prescriptionVersionId` quando há plano, além de mapas por
  código de nutriente: `prescribed`, `planned` e `realized`. Cada valor tem
  `amount` e `unit`. Quando há plano, códigos ausentes em um dos três mapas são
  preenchidos com zero. Sem plano, somente o realizado é retornado.

## Execuções

As rotas abaixo começam em `/api/v1/patients/me/meal-executions`:

| Método e caminho | Ação |
| --- | --- |
| `GET ?date=YYYY-MM-DD` | Listar execuções ativas na data |
| `GET /{id}` | Consultar execução, inclusive seu estado de remoção |
| `POST` | Criar execução |
| `PUT /{id}` | Substituir os itens consumidos |
| `POST /{id}/move` | Mover para outra data/refeição |
| `DELETE /{id}` | Remover temporariamente |
| `POST /{id}/restore` | Restaurar com token |

Criação e substituição recebem `date`, `time` (`HH:mm`), `mealType`
(`BREAKFAST`, `LUNCH`, `SNACK` ou `DINNER`), `plannedMealId` opcional e `items`.
Execuções sem refeição planejada exigem `time` e `mealType`. Quando há
`plannedMealId`, a categoria vem da refeição (um `mealType` divergente é
rejeitado) e `time` assume o horário planejado se omitido; `time` pode ser
editado em `PUT`. Na realocação, `time` e `mealType` omitidos preservam os
valores atuais (ou os da refeição de destino). A resposta devolve ambos.
Cada item recebe `foodId`, `portionId`, `unit`, `quantity`, `multiplier`,
`plannedItemId` opcional e `allowedSubstitutionId` opcional. Quantidade e
multiplicador devem ser positivos. Os alimentos e porções devem estar ativos;
a porção deve pertencer ao alimento. O servidor deriva conversão, kcal e
nutrientes e persiste os valores calculados como snapshots.

Uma execução vinculada usa o modo congelado no plano. Em `RESTRICTED`, mantém
os alimentos planejados; em `SUBSTITUTIONS_ONLY`, aceita somente alimentos
planejados ou substituições autorizadas; em `FREE`, aceita alimentos ativos.
Uma execução sem refeição pode ser criada em qualquer data somente se a
prescrição publicada atual estiver em `FREE`. Sua versão é salva na execução.

Criação, substituição, realocação e remoção exigem `Idempotency-Key`. As três
últimas também exigem `If-Match` com a versão da execução. Repetir o mesmo
comando e chave devolve a resposta original. Versão, destino ocupado ou chave
conflitante retornam `409`. A realocação valida o destino antes de gravar;
`items` pode ser enviado para vincular os itens à nova refeição. Sem `items`,
os itens consumidos são preservados, e vínculos com planejados são removidos
ao mover para uma data livre.

`DELETE` devolve `restoreToken` e `restoreExpiresAt`. `POST /{id}/restore`
recebe `{ "restoreToken": "uuid" }`, é idempotente com o mesmo token e só
funciona por cinco segundos após a remoção.

## Metas e valores nutricionais

O conteúdo de cada versão da prescrição aceita `nutritionTargets`, uma lista
de `{ "nutrientCode": "protein", "amount": 75, "unit": "g" }`. A versão
publicada congela as metas. O planejado é congelado quando os itens do plano
são gravados; o realizado é congelado quando os itens consumidos são gravados.
O catálogo mantém uma unidade consistente por código de nutriente, e metas
devem usar essa unidade quando o código já existe no catálogo.
