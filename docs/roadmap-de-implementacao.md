# Roadmap de implementação e decisões por ticket

Resultado da sessão de planejamento de 2026-11-02. Cada ticket ganha um branch e um PR (`lucasthiele0/luc-NN-slug`). Itens sem ticket ganham um antes de começar. Mudanças que exigem trabalho nos clientes estão em `portal-web/docs/mudancas-de-servidor.md` e `mobile/prato_em_dia_mobile/docs/mudancas-de-servidor.md`.

A regra das migrations em duas etapas (ADR 0003) só vale a partir do go-live. A ordem abaixo aproveita a janela em que mudanças destrutivas ainda são baratas.

## Onda 1: destrava o portal (sem migration)

- **LUC-64** `GET /nutritionists/me/patients/{patientId}/prescription-summary`, com a resposta proposta no ticket. Os totais da versão são totais de um dia, e `suggestedTime` das refeições prescritas continua na resposta. A escala de nutrientes (`nutriente × quantity / portion.quantity`) vira comportamento de `FoodPortion` em `catalog`, com uma porta de consulta em lote de alimentos e porções usada por `tracking` e `prescriptions`. A refatoração do `FoodLogUseCase` precisa de teste de regressão que garanta números idênticos.

## Onda 2: domínio, enquanto migrations destrutivas são grátis

Ordem: LUC-40, LUC-39 com LUC-53, LUC-38 com LUC-29, e por fim LUC-35, LUC-36 e LUC-41.

- **LUC-40** remove o horário só de `planned_meal` e do `PATCH` do plano. O `suggested_time` da prescrição fica.
- **LUC-39 e LUC-53** geração às 08:00, 08:10 e 08:20 BRT (`0 0,10,20 8 * * *`, cada execução processa quem configurou aquele dia da semana, `generateFor` é idempotente) e confirmação às 00:05 (`0 5 0 * * *`). O período deixa de ser fixo em 7 dias (`ck_weekly_plan_period`). O texto do e-mail passa a dizer "na virada do dia".
- **LUC-38 e LUC-29** ver ADR 0002.
- **LUC-35** remove `ux_meal_execution_active_meal`. Todas as execuções mantêm `planned_meal_id` quando há plano. Sem plano, ficam livres e agrupadas por `meal_date` e `meal_type`. A checagem "todo item planejado deve estar na execução" (`validateItems`) é relaxada, e a cobertura passa a ser a união das execuções ativas.
- **LUC-36** estado derivado na leitura. Em `RESTRICTED` e `SUBSTITUTIONS_ONLY`: `NOT_LOGGED`, `PARTIAL` (algum item planejado sem execução) e `LOGGED` (todos cobertos). No `FREE` não há `PARTIAL`: qualquer execução ativa é `LOGGED`.
- **LUC-41** categoria sem itens: bloqueia registro em `RESTRICTED` e `SUBSTITUTIONS_ONLY`, aceita registro livre em `FREE`. Exposta pela nova rota `GET /patients/me/daily-meal-categories?date=`, que devolve as quatro categorias com `mealType`, `state`, `canRegister`, `blockedReason`, `plannedMealId` e itens cobertos e não cobertos.

## Onda 3: acesso e segurança

- **LUC-54** ver ADR 0001. Inclui o esquema OpenAPI de cookie, a lista de origens permitidas e a troca do `@SecurityRequirement(BEARER_AUTH)`.
- **Versões:** `X-Client-Version`, versão mínima por plataforma e `426 CLIENT_OUTDATED`.
- **Aceite de termos:** tabela `consent_record` (conta, tipo do documento, versão, data e hora). O servidor bloqueia a API com `CONSENT_REQUIRED` até haver aceite da versão vigente, no estilo do `PasswordChangeRequiredFilter`. A rota de aceite e o logout ficam liberados. Vale para paciente e nutricionista. Cookies de sessão não exigem banner. Os textos jurídicos e o enquadramento (base legal, eventual exigência do comitê de ética do TCC) são decisão do produto e da instituição.
- **Outbox de e-mail (LUC-43):** tabela com destinatário, tipo, tentativas, `next_attempt_at` e status (`PENDING`, `SENT`, `FAILED`, `DEAD`), gravada na mesma transação, com um job por minuto. Falha de servidor (timeout, 5xx, 429) reagenda +10 minutos até três tentativas, depois `DEAD` (o incidente crítico). Falha permanente (4xx do Resend) vai direto para `FAILED`. E-mails de plano guardam o corpo. **E-mails de acesso guardam só uma referência** (tipo `ACCESS` e a conta ou vínculo): a cada tentativa o envio gera uma nova senha temporária, grava o hash e monta o corpo na hora. Travas: só regenera enquanto a senha inicial não foi usada (`passwordChangeRequired`); o texto diz que a senha substitui qualquer envio anterior; a nova senha só é gravada na tentativa, na mesma transação. Falha de validação na geração do plano (regra da prescrição) não repete e gera um e-mail ao paciente pedindo que fale com o nutricionista.
- **Admin (LUC-32):** `POST /admin/nutritionists` (só `ADMIN`; e-mail, nome e registro profissional opcional; senha temporária gerada pelo servidor e enviada por e-mail, nunca devolvida), `POST /admin/nutritionists/{id}/access-email` (reenvio com nova senha temporária), `GET /admin/users` (filtros e paginação), `GET /admin/users/{id}` e `PATCH /admin/users/{id}/status`. O `POST /identity/useraccounts` é removido. O fluxo de criação de paciente pelo nutricionista (`CreateRelationshipUseCase`) não muda. O status do e-mail do nutricionista é lido do outbox, e o do paciente continua na coluna do vínculo, mantida em sincronia.
- **Auditoria (LUC-33):** tabela `audit_event` gravada na mesma transação das ações de administração (criar nutricionista, reenviar acesso, mudar status) e `GET /admin/audit-events`. Não audita login, vínculo ou publicação por enquanto.
- **Fora do escopo agora:** assinatura simulada (LUC-31) e `GET /admin/metrics`.

## Onda 4: leituras e funcionalidades

- **LUC-28** leituras do nutricionista, somente leitura e com vínculo ativo, espelhando as do paciente: `/nutritionists/me/patients/{patientId}/meal-executions` (por `from` e `to`), `meal-executions/{id}`, `daily-meal-categories?date=`, `daily-nutrition-summary?date=` e `calendar-periods`. Não há rota agregada nova. Esse era o "problema de acesso" do ticket: as rotas atuais exigem o papel `PATIENT`.
- **LUC-30** lista de compras persistida por plano. `POST /patients/me/weekly-plans/{planId}/shopping-list` cria a partir das refeições planejadas ativas (idempotente, `201` ou `200`), e `GET` devolve a lista ativa ou `404`. Qualquer mudança nas refeições planejadas do plano (edição, troca de substituição, regeneração que substitui dias, exclusão) desativa a lista na mesma transação. Índice único parcial `(weekly_plan_id) WHERE active`. Consolida por alimento prescrito, porção e unidade, sem decompor pratos em ingredientes. O "comprado" fica só no app.
- **LUC-50 e LUC-27** mantêm o envelope de erro atual (`status`, `code`, `message`, `errors[]`), que não é RFC 9457. Um catálogo de códigos (código, status HTTP, significado, rotas) documentado no OpenAPI, com um teste que falha se algum `CodedApplicationException` não estiver no catálogo. Entram também `CLIENT_OUTDATED`, `CONSENT_REQUIRED`, o motivo `NO_ALLOWED_FOODS` e o `409` de geração concorrente.
- **LUC-17** o plano já valida o modo da prescrição e a substituição autorizada na edição (`WeeklyPlanningUseCase`). Resta o UX no mobile. No servidor, só testes de verificação.

## Onda 5: infraestrutura antes do piloto

LUC-47 (perfis e `ddl-auto=validate`), LUC-46 (Actuator em porta de gerenciamento), ShedLock, LUC-48 (pipeline com blue/green), backup, observabilidade e endurecimento da VPS. Detalhes em `docs/infraestrutura.md`. A partir daqui vale a regra das duas releases (ADR 0003).

## No fim

- **LUC-49** atualizar os docs. Erros conhecidos: o doc de arquitetura diz que os erros usam `application/problem+json`; o plano consolidado diz `query` quando o parâmetro é `q`; os docs descrevem V001 a V013 e eventos append-only enquanto o código está na V025 com `meal_execution`; `docs/openapi.md` descreve o fluxo de Authorize por token, que cai com os cookies; o Marco 5 cita "assinatura simulada".
- **LUC-26** teste do ciclo completo, se sobrar tempo.

## Ainda em aberto

- **LUC-20** (recuperação após desvio) continua sem definição de produto e não bloqueia nada aqui.
- **Limitação de tentativas de login:** não foi discutida, e a VPS ficará pública. Tem ticket próprio, sem decisão de desenho ainda.

## Tickets criados nesta sessão

| Ticket | Assunto | Onda |
|---|---|---|
| LUC-65 | Aceite versionado de termos e política de privacidade (`consent_record`) | 3 |
| LUC-66 | Outbox de e-mails com retentativa e status `DEAD` | 3 |
| LUC-67 | ShedLock para jobs agendados (blue/green) | 5 |
| LUC-68 | Backup diário criptografado com teste de restauração | 5 |
| LUC-69 | Observabilidade com Grafana Cloud e alertas no Discord | 5 |
| LUC-70 | `X-Client-Version` e `426 CLIENT_OUTDATED` | 3 |
| LUC-71 | Rota `daily-meal-categories` | 2 |
| LUC-72 | Endurecimento da VPS | 5 |
| LUC-73 | Limitação de tentativas de login | sem desenho |

Os demais itens usam os tickets existentes, que receberam um comentário com as decisões.
