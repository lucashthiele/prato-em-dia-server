# Importacao do catalogo TACO

A implementacao fica na feature `importer`. O suporte a TACO (leitor, parser, orquestracao e historico de execucoes) e
isolado por fonte; a feature `catalog`
expoe somente a operacao generica que sincroniza alimentos importados. Novas fontes podem reutilizar essa operacao sem
acessar a persistencia do catalogo.

A importacao e uma rotina operacional ativada somente pelo profile `catalog-import`. O caminho da planilha sempre e
informado externamente. O modo dry-run e o padrao seguro e nao altera alimentos nem registra uma execucao em
`catalog_import_run`. Esse profile executa sem servidor HTTP e possui uma chave interna apenas para inicializar os beans
compartilhados de identidade, portanto nao exige `JWT_SECRET`.

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local,catalog-import \
  -Dspring-boot.run.arguments="--catalog.import.file=../Taco_4a_edicao_2011.xls --catalog.import.dry-run=true"
```

Depois de revisar o relatorio, execute a carga com:

```bash
./mvnw spring-boot:run \
  -Dspring-boot.run.profiles=local,catalog-import \
  -Dspring-boot.run.arguments="--catalog.import.file=../Taco_4a_edicao_2011.xls --catalog.import.dry-run=false"
```

As configuracoes normais de conexao Spring (`spring.datasource.*`) determinam o banco de destino. Nenhum caminho de
maquina ou dado da TACO fica em migration.

Os nutrientes sao associados a uma porcao padrao de 100 g e persistidos em
`food_portion_nutrient`, com sua unidade e valor bruto. `Tr` permanece como `TRACE`, `NA` como `NOT_APPLICABLE`, e
celulas vazias ou `*` como
`NOT_AVAILABLE`. A correcao conhecida `,0,02` da piridoxina do alimento 373 e convertida explicitamente para `0.02`.
Residuos negativos de carboidrato de ate
`0.1 g/100 g`, resultantes do calculo por diferenca da planilha, sao normalizados para zero. Em ambos os casos, o valor
bruto e mantido para auditoria.
