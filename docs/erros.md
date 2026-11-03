# Contrato de erros

Toda resposta de erro da API usa o mesmo envelope JSON. **Ele não é o RFC 9457**
(`application/problem+json`): o `Content-Type` é `application/json` e os campos são
estes:

```json
{
  "status": 409,
  "code": "PLANNING_CONFLICT",
  "message": "Texto em português para exibir ou registrar.",
  "errors": [
    { "field": "items[0].quantity", "code": "REQUIRED_FIELD", "message": "..." }
  ]
}
```

- `status`: o mesmo status HTTP da resposta.
- `code`: identificador estável. **Os clientes decidem pelo `code`**, nunca pelo texto de `message`.
- `message`: texto em português, sem detalhes internos.
- `errors[]`: erros por campo (`field`, `code`, `message`), preenchido nas falhas de validação.

## Catálogo de códigos

O catálogo (código, status HTTP, significado e rotas) é publicado na especificação
OpenAPI (`info.description` e `info.x-error-catalog`, ver [openapi.md](openapi.md)). Ele é
montado a partir das classes `*ErrorCatalogSection` de cada feature
(`features/<feature>/presentation/exception`) e das de `shared/errorcatalog`.

Para criar um código novo:

1. crie a exceção (de preferência estendendo `CodedApplicationException`) e o mapeamento no handler da feature;
2. adicione uma linha em `<Feature>ErrorCatalogSection` (ou crie a classe, com `@Component` e construtor sem argumentos);
3. rode `./mvnw test`: o `ErrorCatalogTest` falha se uma exceção com código não estiver no catálogo.

Códigos que não são erros HTTP (como `NO_ALLOWED_FOODS`, valor de `blockedReason` em
`daily-meal-categories`) entram no catálogo como `REASON`, sem status.

## Parâmetros de consulta

O parâmetro de busca de alimentos é `q` (`GET /api/v1/foods?q=`), nunca `query`.

## Compatibilidade de clientes

Web e mobile enviam `X-Client-Version`. Abaixo da versão mínima da plataforma o
servidor responde `426` com `CLIENT_OUTDATED`; a política está em
[infraestrutura.md](infraestrutura.md).
