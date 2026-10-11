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

Use `POST /api/v1/auth/login` para obter um `accessToken`. No Swagger UI,
selecione **Authorize** e informe somente o token, sem o prefixo `Bearer`.
O Swagger adiciona o prefixo ao header `Authorization` automaticamente.

As operações `login` e `refresh` aparecem como públicas. As demais operações
declaram o esquema `bearerAuth`, de acordo com a autenticação JWT do servidor.

O contexto e o registro append-only da tela de alimentação estão descritos em
[food-log.md](food-log.md); os schemas efetivos permanecem gerados a partir dos
DTOs do controller.
