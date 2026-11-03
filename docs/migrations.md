# Migrations

As migrations do Flyway ficam em `src/main/resources/db/migration` e **nunca são
alteradas depois de integradas**: o Flyway valida o checksum, e o CI
(`scripts/check-migrations.sh`) rejeita migration existente alterada, removida ou
renomeada, nome fora do padrão `V<versão>__<descrição>.sql` e versão duplicada.

A partir do go-live vale a regra de duas etapas do [ADR 0003](adr/0003-blue-green-e-migrations-em-duas-etapas.md):
toda migration precisa ser compatível com a versão anterior da aplicação.

## Histórico (V001 a V025)

| Versões | Assunto |
| --- | --- |
| V001, V002 | Identidade (contas) e refresh tokens |
| V003, V004 | Perfis e vínculos entre nutricionista e paciente |
| V005 a V010 | Catálogo de alimentos, origem, fonte, nutrientes e porções |
| V011 | Prescrições e versões |
| V012 | Planejamento semanal |
| V013 | Eventos de conclusão de refeição (append-only), **removidos na V018** |
| V014, V024 | Busca do catálogo (índice e trigrama) |
| V015, V021 | Metas nutricionais da prescrição e suas unidades |
| V016 | `meal_execution`: execuções mutáveis, que substituem os eventos de conclusão |
| V017, V019 | Snapshots de nutrientes planejados e unidades consistentes |
| V018 | Remove `meal_completion_event` (tabela, trigger e tipo) |
| V020 | Respostas idempotentes (snapshot) |
| V022 | Horário e tipo da execução |
| V023 | Exclusão lógica do plano semanal |
| V025 | `updated_at` da versão da prescrição |

O modelo atual do diário é `meal_execution`; não existe mais o modelo append-only de
`completion-events`.

## Faixas reservadas por trabalho em andamento

| Faixa | Uso |
| --- | --- |
| V026 a V059 | Domínio, registro, leituras, acesso e administração |
| V060 a V069 | Infraestrutura (V060 = tabela `shedlock`) |
