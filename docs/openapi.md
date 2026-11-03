# OpenAPI e Swagger UI

A especificação é gerada pelo Springdoc diretamente dos controllers, DTOs e
anotações de validação da aplicação. Portanto, não há um arquivo estático para
manter manualmente: ao iniciar uma nova versão do servidor, a documentação já
reflete o código compilado.

Com a aplicação em execução, estão disponíveis sem autenticação:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI em JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI em YAML: `http://localhost:8080/v3/api-docs.yaml`

Para salvar uma cópia da especificação gerada:

```shell
curl --fail http://localhost:8080/v3/api-docs.yaml --output openapi.yaml
```

## Autenticação no Swagger

A sessão é um cookie `HttpOnly` (ADR 0001). Faça `POST /api/v1/auth/login` pelo
próprio Swagger UI (mesma origem da API): o navegador guarda o cookie
`access_token` e o envia nas chamadas seguintes, sem botão **Authorize**.
Em HTTP local, defina `SESSION_COOKIE_SECURE=false` em `local.env`; caso
contrário o navegador descarta o cookie `Secure`. A origem do Swagger
(`http://localhost:8080`) está em `ALLOWED_ORIGINS` por padrão.

As operações `login` e `refresh` aparecem como públicas. As demais declaram o
esquema `cookieAuth` (`apiKey` em cookie `access_token`). O header
`Authorization: Bearer` não é mais aceito. O contrato para os clientes está em
[contrato-sessao-cookies.md](contrato-sessao-cookies.md).

O contexto e o registro append-only da tela de alimentação estão descritos em
[food-log.md](food-log.md); os schemas efetivos permanecem gerados a partir dos
DTOs do controller.
