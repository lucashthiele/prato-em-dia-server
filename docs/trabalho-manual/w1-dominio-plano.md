# Trabalho manual: W1 (domínio do plano)

Itens que o código não resolve sozinho. Cada um é checável.

## LUC-40 (horário só da execução)

- [ ] Portal e mobile: parar de enviar e de ler `time` em `PATCH /weekly-plans/{planId}` e na resposta do plano. O servidor ignora o campo se ele ainda vier no corpo. Registrar em `portal-web/docs/mudancas-de-servidor.md` e `mobile/prato_em_dia_mobile/docs/mudancas-de-servidor.md`.
- [ ] Mobile: ao registrar uma execução de refeição planejada, enviar sempre `time`. O servidor não tem mais horário planejado como valor padrão: sem `time` na requisição, só vale o da execução anterior, senão responde 400 (`Meal time and meal type are required`).
- [ ] O `suggested_time` da prescrição continua em `prescribed_meal` e na resposta da prescrição. Conferir na tela de prescrição do portal que ele ainda aparece.

## LUC-39 e LUC-53 (agendamento)

- [ ] No ambiente de homologação, deixar o servidor ligado às 08:00 BRT e conferir nos logs que `generate` roda às 08:00, 08:10 e 08:20 e que só a primeira cria plano (as outras não geram e-mail).
- [ ] Conferir que a confirmação roda às 00:05 BRT e que planos `PENDING` com prazo vencido passam a `CONFIRMED`.
- [ ] Conferir o texto do e-mail de geração em uma caixa real (Resend): deve dizer "até a virada do dia DD/MM" e "Na virada do dia, o plano é confirmado automaticamente".
- [ ] Com mais de uma instância do servidor (blue/green), os jobs rodam em todas até o ShedLock (LUC-67) entrar. A geração é segura (índice único), mas o e-mail só sai de quem criou o plano.

## LUC-38 e LUC-29 (regeneração e duplicatas)

- [ ] Antes do deploy da V032 em qualquer banco com dados reais: fazer backup. A migration marca como histórico (`superseded_at`) as refeições duplicadas mais antigas e as de planos excluídos. Depois, conferir: `SELECT patient_id, meal_date, meal_type, count(*) FROM planned_meal WHERE superseded_at IS NULL GROUP BY 1,2,3 HAVING count(*) > 1;` deve voltar vazio.
- [ ] Mobile: tratar `409 WEEKLY_PLAN_GENERATION_CONFLICT` na regeneração (tentar de novo depois de alguns segundos) e passar a ler só o `meals` ativo do plano: o servidor omite as refeições substituídas, então o `periodStart`/`periodEnd` de um plano parcialmente regenerado pode ter dias sem refeição.
- [ ] Mobile e portal: o `POST /patients/me/weekly-plans` agora é a regeneração (hoje até o próximo dia automático), não mais "sete dias a partir de amanhã". Ajustar textos da tela de regeneração.
- [ ] Decisão de produto a confirmar: um paciente sem preferência cadastrada usa segunda-feira como dia automático na regeneração (mesmo padrão que `GET /weekly-planning-preference` devolve).
- [ ] Verificação em dispositivo: gerar um plano, regenerar no meio da semana e conferir na agenda do app que não há refeições duplicadas e que o histórico não conta na conclusão do plano ativo.
