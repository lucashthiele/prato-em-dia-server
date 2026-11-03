# OpenAPI e Swagger UI

A especificação é gerada pelo Springdoc diretamente dos controllers, DTOs e
anotações de validação da aplicação. Portanto, não há um arquivo estático para
manter manualmente: ao iniciar uma nova versão do servidor, a documentação já
reflete o código compilado.

Com a aplicação em execução, estão disponíveis sem autenticação:

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI em JSON: `http://localhost:8080/v3/api-docs`
- OpenAPI em YAML: `http://localhost:8080/v3/api-docs.yaml`

Em produção o Swagger UI fica desligado (`springdoc.swagger-ui.enabled=false` no
perfil `prod`); o JSON da especificação continua disponível.

Para salvar uma cópia da especificação gerada:

```shell
curl --fail http://localhost:8080/v3/api-docs.yaml --output openapi.yaml
```

## Autenticação no Swagger

A sessão usa cookies `HttpOnly` (ADR [0001](adr/0001-sessao-por-cookies-httponly.md)).
O servidor não devolve tokens no corpo de `POST /api/v1/auth/login` nem aceita
`Authorization: Bearer`. **Não existe o botão Authorize com token**: o fluxo
antigo (copiar o `accessToken` e colá-lo no Authorize) deixou de funcionar.

Para chamar rotas protegidas no Swagger UI:

1. Abra `POST /api/v1/auth/login`, escolha **Try it out**, informe e-mail e senha
   e execute. O navegador guarda os cookies `access_token` e `refresh_token`.
2. Chame qualquer rota protegida. O Swagger UI é servido na mesma origem da API
   e, por isso, o navegador reenvia o cookie sozinho.
3. Para encerrar, use `POST /api/v1/auth/logout`.

Pelo `curl`, use um cookie jar:

```shell
curl -c jar.txt -H 'Content-Type: application/json' \
  -d '{"email":"...","password":"..."}' http://localhost:8080/api/v1/auth/login
curl -b jar.txt http://localhost:8080/api/v1/me
```

No perfil `local` o atributo `Secure` dos cookies é desligado, porque o navegador
não reenvia cookie `Secure` por HTTP.

> Enquanto a migração para cookies (LUC-54) não estiver integrada ao branch, o
> servidor ainda aceita o fluxo antigo por Bearer. Este documento descreve o
> estado final decidido no ADR 0001.

## Erros

Toda resposta de erro usa o envelope `{status, code, message, errors[]}`, descrito
em [erros.md](erros.md). O schema `ApiErrorResponse` e as respostas `4XX` e `5XX`
de cada operação aparecem na especificação, e o **catálogo de códigos** (código,
status HTTP, significado e rotas) está na descrição da API (`info.description`)
e, em formato estruturado, em `info.x-error-catalog`.

## Busca e paginação do catálogo

`GET /api/v1/foods` aceita `q` (busca por nome), `page` (padrão `0`) e `size`
(padrão `20`, máximo `100`).

O contexto e o registro do diário alimentar (execuções mutáveis) estão descritos em
[food-log.md](food-log.md); os schemas efetivos permanecem gerados a partir dos
DTOs do controller.
