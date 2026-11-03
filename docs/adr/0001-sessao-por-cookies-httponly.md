# Sessão por cookies HttpOnly para web e mobile

Status: aceita (2026-11-02)

O backend devolvia access e refresh token no corpo JSON, e o portal os guardava em `localStorage` (exposto a XSS). Decidimos emitir os dois tokens em cookies `HttpOnly` para **todos os clientes**, portal e app Flutter, sem distinção por header, e remover os tokens do corpo de `/auth/login` e `/auth/refresh`.

## Decisões

- **Cookies:** `HttpOnly`, `Secure`, `SameSite=Strict`. `access_token` com `Path=/` e vida igual ao TTL do access token; `refresh_token` com `Path=/api/v1/auth` (enviado só em login, refresh e logout) e vida igual ao TTL do refresh. Nomes simples, sem prefixo `__Host-`, que conflitaria com o `Path` do refresh. `Secure` é desligado só no perfil `local`, por propriedade, porque o `cookie_jar` do Dio e os navegadores não reenviam cookie `Secure` por HTTP.
- **Resource Server:** lê o access token **somente** do cookie. O header `Authorization: Bearer` deixa de ser aceito. O esquema OpenAPI `bearerAuth` vira um esquema de cookie. O Swagger UI, servido na mesma origem da API, usa o cookie recebido no login.
- **CSRF:** checagem de `Origin` em métodos que mudam estado (`POST`, `PUT`, `PATCH`, `DELETE`), contra uma lista de origens permitidas por ambiente (portal e a própria origem da API, para o Swagger). Requisições sem `Origin` são aceitas, porque navegadores sempre o enviam em escritas entre origens e o app nativo não o envia. Não há token CSRF.
- **CORS:** não configurado. O portal fala com a API pelo mesmo origin (Vite em desenvolvimento, Caddy em produção, ver ADR 0008 do portal).
- **Refresh:** o refresh token continua rotacionado e de uso único. Duas renovações simultâneas (abas, requisições paralelas) fazem a segunda falhar com `401`. O servidor não ganha janela de tolerância. Os clientes renovam de forma single-flight e repetem uma vez se a renovação falhar.

## Alternativas descartadas

- **Cookies só para o portal, JSON para o mobile** (marcação por header): dois mecanismos para manter, com o cabeçalho decidindo o formato de entrega.
- **Cookie mais Bearer como reserva:** dois caminhos de autenticação, e quem só usa Bearer não teria de onde obter o token, já que o corpo não o traz.
- **Token CSRF por double-submit:** exigiria trabalho no portal e no mobile sem ganho sobre a checagem de `Origin` com `SameSite=Strict`.
- **CORS com credenciais:** só faria sentido com o portal em outra origem.

## Consequências

- É uma mudança incompatível para os dois clientes, liberada porque ainda não há produção. Depois do go-live, mudanças de contrato seguem a política de compatibilidade em duas releases.
- Se o portal um dia sair do mesmo origin da VPS, reabrimos CORS com credenciais e a política de `SameSite`.
- Testes de integração, `curl` e o dev seed passam a usar cookies.
