# Prato em Dia — Server

API do **Prato em Dia**, uma aplicação para conectar nutricionistas e pacientes,
gerenciar perfis, catálogo de alimentos, prescrições, planejamento semanal e
diário alimentar.

A API é exposta sob `/api/v1`. O servidor é uma aplicação Spring Boot organizada por funcionalidades e com
separação entre domínio, aplicação, infraestrutura e apresentação.

## Funcionalidades

- autenticação stateless com access token JWT e refresh token;
- perfis de nutricionistas e pacientes;
- vínculo entre nutricionista e paciente, com envio de acesso por e-mail;
- catálogo de alimentos, porções e informações nutricionais, com busca remota
  por nome (`q`) em `GET /api/v1/foods`;
- criação e versionamento de prescrições alimentares, com metas nutricionais;
- planejamento semanal de refeições pelo paciente, com preferências, geração
  agendada de planos e confirmação;
- diário alimentar: execuções de refeições mutáveis (planejadas ou livres),
  calendário de períodos consultáveis e resumo nutricional diário;
- importação de catálogos TACO e TBCA;
- documentação interativa da API com OpenAPI e Swagger UI.

## Tecnologias

- Java 25;
- Spring Boot 4 e Maven;
- Spring Security e OAuth2 Resource Server;
- PostgreSQL, Spring Data JPA e Flyway;
- Docker Compose;
- JUnit, ArchUnit, Testcontainers e JaCoCo;
- Springdoc OpenAPI;
- Resend para envio de e-mails.

## Pré-requisitos

- JDK 25;
- Docker com Docker Compose.

Não é necessário instalar o Maven: o projeto inclui o Maven Wrapper.

## Executando localmente

1. Crie `local.env` na raiz do projeto e defina uma chave JWT com pelo menos
   32 bytes e sua chave de API do Resend:

   ```properties
   JWT_SECRET=troque-esta-chave-local-por-uma-chave-segura
   RESEND_API_KEY=re_sua_chave
   ```

2. Inicie a aplicação:

   ```shell
   ./mvnw spring-boot:run
   ```

O suporte a Docker Compose do Spring Boot inicia automaticamente o PostgreSQL
definido em `compose.yaml`. As migrations do Flyway são aplicadas na
inicialização.

Depois que o servidor estiver pronto, os principais endereços locais são:

| Serviço | Endereço |
| --- | --- |
| API | `http://localhost:8080` |
| Swagger UI | `http://localhost:8080/swagger-ui.html` |
| OpenAPI JSON | `http://localhost:8080/v3/api-docs` |
| PostgreSQL | `localhost:5432` |

Para encerrar os serviços locais iniciados pelo Compose:

```shell
docker compose down
```

### Configuração

A aplicação lê automaticamente o arquivo ignorado `local.env` como um arquivo
de propriedades. Além de `JWT_SECRET`, as principais variáveis opcionais são:

| Variável | Valor padrão | Finalidade |
| --- | --- | --- |
| `JWT_ISSUER` | `prato-em-dia` | Emissor dos tokens |
| `JWT_ACCESS_TOKEN_TTL` | `PT15M` | Duração do access token |
| `JWT_REFRESH_TOKEN_TTL` | `P30D` | Duração do refresh token |
| `MAIL_FROM` | `onboarding@resend.dev` | Remetente dos e-mails; use um domínio verificado no Resend para enviar a outros destinatários |
| `APP_URL` | `http://localhost:3000` | URL usada nos links enviados por e-mail |
| `SERVER_PORT` | `8080` | Porta HTTP da aplicação |

`RESEND_API_KEY` é obrigatória. O envio de e-mails é agendado após a criação do
vínculo ou a solicitação de reenvio. O status começa como `PENDING` e passa a
`SENT` ou `FAILED` quando a chamada ao Resend termina. `SENT` indica que o
Resend aceitou a mensagem, não que ela foi entregue à caixa de entrada.

Quando um plano semanal é criado, seja pela geração automática ou pela geração
manual (inclusive após excluir o plano), o paciente recebe um e-mail com período,
quantidade de refeições e prazo de confirmação, sem nomes de alimentos. O envio
só ocorre após o commit, não é rastreado e falhas são apenas registradas em log.
Edição, confirmação e exclusão do plano não enviam e-mail.

As propriedades padrão do Spring, como `SPRING_DATASOURCE_URL`,
`SPRING_DATASOURCE_USERNAME` e `SPRING_DATASOURCE_PASSWORD`, podem ser usadas
para conectar a outro PostgreSQL. Nesse caso, use também
`SPRING_DOCKER_COMPOSE_ENABLED=false` se os containers locais não forem
necessários.

## Testes e build

Execute a suíte completa, as verificações de arquitetura e a geração do
relatório de cobertura com:

```shell
./mvnw clean verify
```

O relatório do JaCoCo será gerado em `target/site/jacoco/index.html`.
Os testes de integração usam Testcontainers e, por isso, precisam de acesso ao
Docker.

## Code formatting

O Spotless é a fonte de verdade da formatação do projeto. O Eclipse JDT aplica
o profile canônico [`eclipse-formatter.xml`](eclipse-formatter.xml), com 4
espaços para indentação Java, continuação de 8 espaços e largura desejada de
120 colunas. Não é necessário usar o IntelliJ IDEA para formatar corretamente.

Para corrigir a formatação automaticamente:

```shell
./mvnw spotless:apply
```

Para validar sem modificar arquivos:

```shell
./mvnw spotless:check
```

A validação completa também verifica a formatação, antes da compilação e dos
testes, e o CI rejeita arquivos fora do padrão:

```shell
./mvnw verify
```

No IntelliJ IDEA, a configuração recomendada em **Settings → Editor → Code
Style → Java** é `Tab size: 4`, `Indent: 4` e `Continuation indent: 8`. Para
usar exatamente o formatter canônico dentro da IDE, instale o plugin *Eclipse
Code Formatter*, selecione o formatter Eclipse nas configurações do plugin e
importe `eclipse-formatter.xml`. Essas preferências são apenas uma facilidade
local; Maven/Spotless continua sendo a autoridade.

Para gerar o artefato executável sem repetir os testes:

```shell
./mvnw package -DskipTests
java -jar target/server-0.0.1-SNAPSHOT.jar
```

## Estrutura do projeto

```text
src/main/java/com/ulvratech/pratoemdia/server/
├── features/
│   ├── catalog/          # alimentos, porções e nutrientes
│   ├── identity/         # contas, autenticação e tokens
│   ├── importer/         # importadores TACO e TBCA/MongoDB
│   ├── planning/         # planejamento semanal e refeições planejadas
│   ├── prescriptions/    # prescrições alimentares e versões
│   ├── profiles/         # perfis de usuário
│   ├── relationships/    # vínculos entre profissionais e pacientes
│   └── tracking/         # diário alimentar, execuções e resumo nutricional
└── shared/               # contratos e componentes compartilhados
```

Cada feature segue, conforme a necessidade, as camadas `domain`,
`application`, `infrastructure` e `presentation`. As migrations ficam em
`src/main/resources/db/migration`.

## Documentação adicional

- [OpenAPI e autenticação no Swagger](docs/openapi.md)
- [Diário alimentar](docs/food-log.md)
- [Dependências de backend do redesign mobile](docs/mobile-redesign-backend-dependencies.md)
- [Importação do catálogo TACO](docs/catalog-taco-import.md)
- [Importação do catálogo TBCA via MongoDB](docs/catalog-mongodb-import.md)
- [Seed de dados para desenvolvimento](docs/dev-seed.md)
- [Desenvolvimento com Git worktrees](docs/worktrees.md)

## Licença

Este projeto é distribuído sob a [PolyForm Noncommercial License 1.0.0](LICENSE). Uso não comercial é livre; uso comercial requer autorização prévia — entre em contato: lhthiele@outlook.com.
