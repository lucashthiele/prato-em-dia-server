# Pedidos do app ao servidor: MVP final (16/11/2026)

Mudanças no servidor que saíram do fechamento das decisões do app
(`mobile/prato_em_dia_mobile/docs/decisoes-mvp.md`). Complementam os pedidos do
portal em [pedidos-mvp-final-2026-11-16.md](pedidos-mvp-final-2026-11-16.md).
Prazo: entrega final do TCC em 17/11/2026.

| ID | Origem | Resumo | Prioridade |
|---|---|---|---|
| P-01 | A-05, Q15 | Glossário: "A registrar" vira "Sem registro" | Necessário (documentação) |
| P-02 | Q1 | Recusar execução sem itens | Recomendado |
| P-03 | Q8 | Prescrição atual com modo, autor e nomes dos alimentos | Recomendado |
| P-04 | Teste E2E | `GET /patients/me/prescriptions/current` responde 500 sempre | **Bloqueante**, aplicado em `fix/p04-prescricao-atual` |

## P-01: Glossário com "Sem registro"

- **O que muda:** em `GLOSSARY.md`, no verbete **Estado da categoria** (linha
  42), trocar "A registrar" por "Sem registro" e listar "A registrar" e "não
  realizada" como termos a evitar.
- **Motivo:** o portal e o app usam **Registrada** (`LOGGED`), **Parcial**
  (`PARTIAL`) e **Sem registro** (`NOT_LOGGED`) (`portal-web/GLOSSARY.md`). O
  servidor é o único que ainda diz "A registrar". O enum não muda.

## P-02: Recusar execução sem itens

- **Comportamento:** `POST /api/v1/patients/me/meal-executions` e
  `PUT /api/v1/patients/me/meal-executions/{id}` com `items` vazio ou ausente
  respondem `400 VALIDATION_ERROR`.
- **Contrato:** hoje nada impede uma execução sem itens (`ExecutionTypes.Write`
  não valida `items`). A validação pode ficar no caso de uso ou no request.
  Catalogar no OpenAPI.
- **Motivo:** o critério de aceite 4 do plano consolidado diz que nenhuma
  execução fica sem alimento. Hoje uma execução vazia vira `LOGGED` numa
  categoria `FREE` e `PARTIAL` nos modos restritos, o que engana a síntese do
  portal. O app deixa de gravar "não realizada" como execução vazia (Q1).
- **Dados existentes:** versões antigas do app gravaram execuções vazias. Para
  a demonstração, basta um seed limpo; nenhuma migration é necessária.
- **Se não der tempo:** o app já não envia execução vazia, então a demonstração
  não depende deste item.

## P-03: Prescrição atual com modo, autor e nomes dos alimentos

- **O que muda:** `GET /api/v1/patients/me/prescriptions/current` devolve só o
  `VersionOutput`. Ele não traz o `planningMode` (que fica em
  `PrescriptionOutput`), nem o nome de quem publicou (só `createdByUserId`),
  nem o nome dos alimentos e porções dos itens e substituições (só `foodId` e
  `foodPortionId`). Pedido: incluir `planningMode`, `nutritionistFullName` e,
  em `ItemOutput` e `SubstitutionOutput`, `foodName` e `portionLabel`. Campos
  novos e opcionais, sem quebrar o portal.
- **Motivo:** a tela de prescrição do app (Q8) mostra autoria, modo e os
  alimentos. Sem isso, o app tira o nome do nutricionista do vínculo
  (`nutritionistFullName`), resolve cada alimento com uma chamada ao catálogo e
  não consegue mostrar o modo da prescrição.
- **Se não der tempo:** o app já lê `planningMode` quando ele vier e funciona
  sem ele, omitindo o modo.

## P-04: Prescrição atual responde 500

- **Sintoma:** no teste E2E do app (servidor local, seed de 16/11/2026),
  `GET /api/v1/patients/me/prescriptions/current` respondeu
  `500 INTERNAL_ERROR` para o paciente do seed.
- **Causa:** `PrescriptionJpaRepository.findCurrentByPatient` (linha 21) faz
  `select distinct p from PrescriptionModel p join p.versions v ... order by
  v.publishedAt desc`. O PostgreSQL recusa `SELECT DISTINCT` com `ORDER BY` em
  coluna fora da lista de seleção ("for SELECT DISTINCT, ORDER BY expressions
  must appear in select list"), então a consulta falha para qualquer paciente,
  com ou sem versão publicada. O bug existe desde `068bc57`.
- **Correção sugerida:** tirar o `distinct` (a prescrição tem no máximo uma
  versão `PUBLISHED`, então o join não duplica linhas) ou ordenar por uma
  expressão agregada. Cobrir com teste de integração no PostgreSQL
  (Testcontainers), já que o H2 ou o mock não pegam o erro.
- **Impacto no app:** sem essa rota, não funcionam a tela de prescrição (Q8),
  o estado sem prescrição (Q24, A-01), as opções de troca na Semana (Q19) nem o
  horário sugerido do registro (Q17). O app degrada e não quebra, mas a
  demonstração depende dela.

## Considerado e descartado

- **Código próprio para "sem prescrição" na geração de plano.** Sem prescrição,
  gerar ou regenerar responde `404 WEEKLY_PLAN_NOT_FOUND`, o mesmo código de
  plano inexistente. O app não depende disso: ele detecta o caso por
  `GET /patients/me/prescriptions/current` (`404 PRESCRIPTION_NOT_FOUND`).
- **`blockedReason` próprio para "sem prescrição".** Pelo mesmo motivo, o
  `NO_ALLOWED_FOODS` único em `daily-meal-categories` basta.
- **Encerrar vínculo pelo paciente.** A rota `DELETE /patients/me/relationship`
  já existe, mas o app não a usa no MVP (Q9).
