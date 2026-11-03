# Aceite versionado de termos (LUC-65)

Tabela `consent_record` (V050): conta, tipo do documento (`TERMS_OF_USE`, `PRIVACY_POLICY`), versão e data/hora do aceite; única por (conta, documento, versão).

## Versão vigente

Configuração `consent.current-versions` (`CONSENT_TERMS_VERSION`, `CONSENT_PRIVACY_VERSION`; padrão `placeholder-1`). Subir a versão de um documento pede novo aceite de todos os pacientes e nutricionistas. `CONSENT_ENFORCED=false` desliga o bloqueio (as rotas de aceite continuam funcionando).

## Bloqueio

Para contas `PATIENT` e `NUTRITIONIST` (o `ADMIN` é isento), enquanto alguma versão vigente não estiver aceita, qualquer rota autenticada responde `403` com `code = CONSENT_REQUIRED`, no estilo do `PASSWORD_CHANGE_REQUIRED`. Ficam liberadas: `GET` e `POST /api/v1/me/consents`, `POST /api/v1/auth/logout`, `POST /api/v1/auth/refresh` e `PATCH /api/v1/me/password` (quem entra com senha temporária troca a senha primeiro e só depois aceita).

## Rotas

- `GET /api/v1/me/consents` → `{ "upToDate": false, "documents": [ { "documentType": "TERMS_OF_USE", "currentVersion": "2", "accepted": false, "acceptedAt": null }, ... ] }`.
- `POST /api/v1/me/consents` com `{ "documentType": "TERMS_OF_USE", "version": "2" }` → mesmo corpo do `GET`, já atualizado. Repetir é inócuo. Versão diferente da vigente: `409 CONSENT_VERSION_MISMATCH`; documento inexistente: `400 VALIDATION_ERROR`.

## Clientes (fora deste repositório)

Portal e mobile: ao receber `CONSENT_REQUIRED`, ir para a tela de aceite (`GET`, exibir os textos, `POST` por documento) e repetir a requisição original. Os textos jurídicos são decisão do produto (ver `docs/trabalho-manual/w3-acesso-admin.md`).
