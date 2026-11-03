# Trabalho manual pendente (final-delivery)

Tudo o que o código não resolve sozinho, por área. Cada arquivo tem checklists por ticket.

| Arquivo | Área | Exemplos |
|---|---|---|
| [w1-dominio-plano.md](w1-dominio-plano.md) | Plano semanal (LUC-40, 39, 53, 38, 29) | backup antes da V032, ajustes no portal e no mobile, conferir crons em homologação |
| [w2-registro-leituras.md](w2-registro-leituras.md) | Registro e leituras (LUC-35, 36, 41, 71, 64, 28, 30) | clientes adotarem as novas rotas e estados |
| [w3-acesso-admin.md](w3-acesso-admin.md) | Acesso e admin (LUC-54, 70, 65, 66, 43, 32, 33, 73) | textos jurídicos, `ALLOWED_ORIGINS`, primeiro ADMIN, cookies no portal e no mobile |
| [w4-infra.md](w4-infra.md) | Infra (LUC-47, 46, 67, 48, 68, 69, 72, 50, 27, 49) | VPS, segredos do CI, backup, Grafana Cloud e Discord |

## Pendências da integração (código)

- [ ] Ligar `CriticalEventMetrics.emailOutboxDead()` ao momento em que o e-mail vira `DEAD`
      (`DispatchDueEmailsUseCase`) e `planGenerationFailed()` à rejeição da geração automática
      (`WeeklyPlanningUseCase.notifyRejected`). Os contadores existem e o alerta do Discord depende deles,
      mas nada os incrementa ainda. Exige injetar o componente e ajustar os testes dos casos de uso.
- [ ] LUC-49: os docs de arquitetura, o plano consolidado e o Marco 5 não estão neste repositório.
      Corrigir lá: `application/problem+json` (o envelope real é `status`, `code`, `message`, `errors[]`),
      `query` vs `q`, migrations V001–V013 vs V025+ com `meal_execution`, e "assinatura simulada" (LUC-31 fora do escopo).
- [ ] Portal web e mobile: registrar as mudanças de contrato em `portal-web/docs/mudancas-de-servidor.md` e
      `mobile/prato_em_dia_mobile/docs/mudancas-de-servidor.md` (cookies, `X-Client-Version`, aceite de termos,
      `time` removido do plano, novas rotas de categorias, resumo da prescrição e lista de compras).
- [ ] Decisões de produto a validar: dia automático da regeneração sem preferência (segunda-feira), prazo de
      confirmação da regeneração (próxima meia-noite), status HTTP 403 do `CONSENT_REQUIRED`, limites do
      rate limit de login (LUC-73).
- [ ] LUC-31 (subscriptions) e `GET /admin/metrics` ficaram fora do escopo.
