# Seed de dados para desenvolvimento

O profile `dev-seed` popula o banco **local** com contas, vinculos, prescricoes, planos semanais e historico
de refeicoes, para que seja possivel navegar pela API e pelo app sem cadastrar tudo a mao. O catalogo de
alimentos nao e alterado: o seed usa alimentos que ja foram importados
(veja [TACO](catalog-taco-import.md) e [TBCA](catalog-mongodb-import.md)).

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed
```

Ao terminar, o comando encerra a aplicacao e gera [dev-seed-credentials.md](dev-seed-credentials.md) com os
e-mails e a senha das contas criadas.

## O que e criado

| Area | Tabelas | Dados |
| --- | --- | --- |
| Identidade | `user_account`, `nutritionist_profile`, `patient_profile` | 1 admin, 2 nutricionistas, 50 pacientes (um com troca de senha obrigatoria, um inativo e 45 em lote) |
| Vinculos | `nutritionist_patient_relationship` | vinculos ativos (e-mail enviado, pendente e com falha) e um encerrado |
| Prescricoes | `prescription`, `prescription_version`, `prescribed_meal`, `prescribed_meal_item`, `allowed_substitution`, `prescription_nutrition_target` | modos `RESTRICTED`, `SUBSTITUTIONS_ONLY` e `FREE`; versoes publicada, arquivada e rascunho; 4 refeicoes por versao; metas de nutrientes |
| Planejamento | `weekly_planning_preference`, `weekly_plan`, `planned_meal`, `planned_meal_item` | semana anterior, atual e proxima (pendente), relativas a data de execucao |
| Diario | `meal_execution`, `meal_execution_item`, `meal_execution_nutrient_snapshot`, `meal_execution_command` | refeicoes planejadas registradas, registros livres e um registro removido |

Os alimentos das prescricoes sao escolhidos no catalogo existente por nome (pao, arroz, feijao, frango etc.) e,
quando nao ha um alimento correspondente, por qualquer alimento ativo que tenha energia na porcao padrao.
Se o catalogo estiver vazio, o seed falha pedindo a importacao.

### Pacientes em lote

Para exercitar a paginacao da lista de pacientes do portal (20 por pagina), a Ana recebe 45 pacientes extras
(`Paciente 01` a `Paciente 45`, e-mails `patient.loteNN@seed.pratoemdia.local`), somando 48 vinculos ativos
(3 paginas). O e-mail do vinculo alterna entre enviado, pendente e com falha, e a data de criacao de cada
vinculo e espalhada por dias distintos, para a ordenacao fazer diferenca. Esses pacientes nao tem prescricao
nem historico de refeicoes.

> Quem ja rodou o seed precisa rodar de novo com `--devseed.reset=true` (veja abaixo) para receber o lote.

## Seguranca

- O seed so executa se o host do `spring.datasource.url` for `localhost`, `127.0.0.1` ou `::1`.
- Todos os e-mails usam o dominio `@seed.pratoemdia.local`, e os ids sao deterministicos. Somente esses
  registros sao removidos pelo reset.

## Reexecutar

- Sem opcoes, se as contas do seed ja existem nada e inserido; apenas o arquivo de credenciais e regerado.
- Para apagar e recriar os dados do seed (e somente eles):

```bash
./mvnw spring-boot:run -Dspring-boot.run.profiles=local,dev-seed \
  -Dspring-boot.run.arguments="--devseed.reset=true"
```

## Propriedades

| Propriedade | Padrao | Descricao |
| --- | --- | --- |
| `devseed.password` | `Seed@12345` | senha de todas as contas |
| `devseed.credentials-file` | `docs/dev-seed-credentials.md` | onde o arquivo de credenciais e escrito |
| `devseed.reset` | `false` | remove os dados do seed antes de inseri-los |

O seed e codigo de ferramenta de desenvolvimento (`com.ulvratech.pratoemdia.server.devseed`), fora de `features`,
e usa SQL direto porque insere registros ja finalizados (prescricoes publicadas) e depende das triggers do banco.
