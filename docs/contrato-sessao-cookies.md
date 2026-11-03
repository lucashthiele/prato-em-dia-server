# Contrato da sessão por cookies (ADR 0001)

Contrato do servidor para o portal web e o app Flutter. O código do portal e do app não faz parte deste repositório.

## Cookies

| Cookie | Path | Vida | Atributos |
|---|---|---|---|
| `access_token` | `/` | TTL do access token (`expiresIn` da resposta) | `HttpOnly`, `Secure`, `SameSite=Strict` |
| `refresh_token` | `/api/v1/auth` | TTL do refresh token | `HttpOnly`, `Secure`, `SameSite=Strict` |

`Secure` vem de `SESSION_COOKIE_SECURE` (padrão `true`; `false` só em desenvolvimento local por HTTP).

## Rotas

- `POST /api/v1/auth/login` (corpo: `email`, `password`): `200` com `{ "expiresIn": 900, "passwordChangeRequired": false }` e dois `Set-Cookie`. O corpo **não** traz tokens.
- `POST /api/v1/auth/refresh`: sem corpo. Lê o cookie `refresh_token`, rotaciona (uso único) e devolve novos cookies. Sem cookie ou com token já usado: `401` (`AUTH_INVALID_REFRESH_TOKEN`). Duas renovações simultâneas fazem a segunda falhar; os clientes renovam em single-flight e repetem uma vez.
- `POST /api/v1/auth/logout`: sem corpo. Revoga o refresh token do cookie e responde `204` com os dois cookies expirados (`Max-Age=0`).
- `PATCH /api/v1/me/password`: o corpo da resposta segue o formato do login e os cookies são renovados.
- O header `Authorization: Bearer` **não** é aceito em nenhuma rota.

## CSRF e CORS

- Em `POST`, `PUT`, `PATCH` e `DELETE`, se a requisição traz `Origin`, ele precisa estar em `ALLOWED_ORIGINS` (lista separada por vírgulas). Caso contrário: `403` com o código `ORIGIN_NOT_ALLOWED`. Sem `Origin` (app nativo, `curl`) a requisição passa.
- Não há token CSRF nem CORS: o portal usa o mesmo origin da API (Vite em desenvolvimento, Caddy em produção).

## Portal web (fora deste repositório)

Remover os tokens do `localStorage`; usar `credentials: "include"` apenas se sair do mesmo origin; restaurar a sessão ao recarregar com `POST /auth/refresh`; renovar em single-flight.

## App Flutter (fora deste repositório)

Guardar os cookies num `cookie_jar` persistente (Dio) e reenviá-los. Em desenvolvimento por HTTP, o servidor precisa de `SESSION_COOKIE_SECURE=false`.
