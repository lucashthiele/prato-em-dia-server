# Importacao do catalogo TBCA via MongoDB

O importer le os documentos produzidos pelo scraper TBCA na collection MongoDB
e sincroniza o catalogo relacional. A operacao e idempotente: alimentos, porcoes
e nutrientes importados usam identificadores deterministas, e uma nova execucao
sem mudancas apenas contabiliza registros ignorados.

O contrato esperado e o mesmo de `alimentos.json`: cada documento possui
`externalId`, `name`, `brand`, `category` e `portions`. Cada porcao possui
`type`, `label`, `rawLabel`, `quantity`, `unit` e o mapa `nutrients`. Deve haver
exatamente uma porcao `STANDARD`; a porcao de 100 g e persistida como qualquer
outra porcao. Os nutrientes sao armazenados em `food_portion_nutrient`, nunca
diretamente em `food`.

As credenciais e opcoes locais ficam no arquivo ignorado `local.env`:

```dotenv
MONGODB_URI=mongodb://usuario:senha@127.0.0.1:27017/?authSource=admin
MONGODB_DATABASE=tbca
MONGODB_COLLECTION=foods
MONGODB_IMPORT_DRY_RUN=true
```

Se o PostgreSQL ja estiver rodando fora do projeto da worktree, configure tambem
`SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`,
`SPRING_DATASOURCE_PASSWORD` e `SPRING_DOCKER_COMPOSE_ENABLED=false` nesse mesmo
arquivo. Isso evita que o Docker Compose da worktree tente abrir novamente as
portas dos containers existentes.

Primeiro execute em dry-run:

```bash
set -a
source local.env
set +a
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,mongodb-catalog-import
```

Depois de revisar o relatorio, altere `MONGODB_IMPORT_DRY_RUN=false` e execute o
mesmo comando. O profile nao inicia servidor HTTP. Uma importacao com qualquer
documento invalido e abortada sem alterar alimentos. Execucoes efetivas ficam
registradas em `catalog_import_run`, sem armazenar a URI (e portanto sem expor
credenciais); apenas `database.collection` e o checksum do conjunto sao salvos.

Os status `VALUE`, `TRACE`, `NOT_AVAILABLE`, `NOT_APPLICABLE` e `NOT_ANALYZED`
sao preservados. A unidade ausente em uma porcao importada tambem e preservada
como `null`; porcoes criadas pela API continuam exigindo unidade.
