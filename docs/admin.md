# Módulo admin e auditoria (LUC-32, LUC-33)

Todas as rotas ficam em `/api/v1/admin/**` e exigem o papel `ADMIN` (`403 ACCESS_DENIED` para os demais). O `POST /api/v1/identity/useraccounts` foi removido; o paciente continua sendo criado pelo nutricionista (`CreateRelationshipUseCase`, sem mudança).

## Rotas

| Rota | O que faz |
|---|---|
| `POST /admin/nutritionists` | Corpo `{ email, fullName, registrationNumber? }`. Cria a conta `NUTRITIONIST` ativa e o perfil, grava a referência do e-mail de acesso na outbox e o evento de auditoria, tudo na mesma transação. `201` com `{ id, email, fullName, registrationNumber, accessEmailStatus: "PENDING" }`. **A senha temporária nunca é devolvida.** E-mail repetido: `409 USER_ACCOUNT_ALREADY_EXISTS`. |
| `POST /admin/nutritionists/{id}/access-email` | `202`. Agenda novo e-mail de acesso (nova senha temporária, que substitui as anteriores). Só para nutricionista ativo que ainda não trocou a senha inicial; senão `409 ACCESS_EMAIL_NOT_AVAILABLE`. |
| `GET /admin/users` | Página (`page`, `size` até 100) com filtros `role`, `status`, `q` (nome ou e-mail). Mais recentes primeiro. |
| `GET /admin/users/{id}` | Detalhe; `404 USER_ACCOUNT_NOT_FOUND`. |
| `PATCH /admin/users/{id}/status` | Corpo `{ "status": "ACTIVE" \| "INACTIVE" }`, `204`. Desativar revoga os refresh tokens (o access token em curso vale até expirar, no máximo 15 min). O admin não altera a própria conta: `409 ADMIN_CANNOT_CHANGE_OWN_STATUS`. |
| `GET /admin/audit-events` | Eventos do mais recente ao mais antigo. Filtros: `action`, `actorId`, `targetId`, `from`, `to` (ISO-8601). Período invertido: `400 AUDIT_INVALID_FILTER`. |

`accessEmail` em `GET /admin/users`: `{ status, attempts, lastError }`. Para nutricionista vem da outbox (`PENDING`, `SENT`, `FAILED`, `DEAD`; `lastError = PASSWORD_ALREADY_CHANGED` quando o nutricionista trocou a senha antes do reenvio). Para paciente vem da coluna do vínculo (`PENDING`, `SENT`, `FAILED`). Vem `null` para `ADMIN`.

## Auditoria

Tabela `audit_event` (V052), gravada na mesma transação da ação: `NUTRITIONIST_CREATED`, `ACCESS_EMAIL_RESENT`, `USER_STATUS_CHANGED` (detalhe `ACTIVE -> INACTIVE`). Não audita login, vínculo nem publicação (decisão do roadmap).

## Primeiro administrador

Não há rota para criar `ADMIN`. A primeira conta é criada à mão no banco (ver `docs/trabalho-manual/w3-acesso-admin.md`); em desenvolvimento o dev-seed já cria uma.
