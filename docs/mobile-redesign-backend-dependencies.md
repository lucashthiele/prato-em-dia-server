# Backend — dependências do redesign mobile

## Resumo

Entregar todos os seis contratos em uma única versão: busca remota do catálogo, períodos de calendário, resumo nutricional diário, metas por prescrição e o novo modelo de execuções mutáveis. O modelo append-only de `completion-events` será removido, sem compatibilidade ou migração de dados de produção.

## Mudanças principais

- Estender `GET /api/v1/foods` com `q` (decisão LUC-45: o nome do parâmetro é `q`, máx. 180 caracteres; termo vazio após normalização retorna 400), mantendo a paginação atual (`page` e `size`); a busca será remota, case/acento-insensitive e limitada a alimentos ativos.
- Criar `GET /api/v1/patients/me/calendar-periods`, retornando intervalos explícitos de planos e prescrições consultáveis, além de datas isoladas com execuções livres, para o calendário não inferir disponibilidade localmente.
- Adicionar metas nutricionais à versão da prescrição: cada meta terá código de nutriente, quantidade e unidade. Os contratos de criar/editar/consultar prescrição passarão a receber e devolver essas metas. O futuro portal poderá preenchê-las automaticamente a partir dos alimentos, mas a prescrição continuará sendo a fonte de verdade publicada.
- Criar `GET /api/v1/patients/me/daily-nutrition-summary?date=YYYY-MM-DD`, com identificação de data/plano/prescrição quando aplicáveis e totais separados de:
  - metas prescritas;
  - composição planejada;
  - consumo realizado.
  Os valores serão calculados e congelados a partir dos nutrientes e porções usados no momento da gravação. Nutrientes ausentes serão considerados `0`, conforme definido. Datas sem plano retornarão realizado sem metas ou comparação.
- Substituir o módulo de tracking por uma entidade mutável `MealExecution`, com data, refeição planejada opcional, versão da prescrição/plano de origem, modo aplicável, itens consumidos e snapshot nutricional.
  - Execuções vinculadas a refeições preservam `RESTRICTED`, `SUBSTITUTIONS_ONLY` e `FREE`.
  - Execuções sem refeição podem existir em qualquer data, mas somente quando a prescrição publicada atual estiver em `FREE`; elas salvam a versão usada para autorização e cálculo.
  - `RESTRICTED` mantém os alimentos planejados; `SUBSTITUTIONS_ONLY` aceita somente substituições autorizadas; `FREE` permite inclusão, remoção e troca de alimentos ativos.
- Publicar CRUD e operações específicas:
  - listar por data e consultar uma execução;
  - criar, substituir todos os campos editáveis e remover;
  - mover atomicamente uma execução para outra data/refeição, ou para uma data livre quando autorizada;
  - remover via soft delete, retornando token de restauração e expiração de cinco segundos;
  - restaurar idempotentemente dentro da janela usando esse token.
  Todas as mutações usarão versão otimista/idempotência e retornarão `409` para versão, destino ou chave conflitantes.
- Criar uma migration posterior que remova tabela, trigger e tipo do modelo `meal_completion_event`, e introduza as tabelas de execuções, itens, snapshots nutricionais, exclusão temporária e metas por versão. Não alterar migrations já existentes.

## Contratos e regras

- As respostas de execução exporão `id`, `version`, data, `plannedMealId` opcional, contexto de modo, itens, totais nutricionais, timestamps e estado de remoção/restauração.
- A realocação será transacional: valida paciente, refeição de destino, data e modo antes de alterar qualquer vínculo. Destino inválido ou atualização concorrente não altera a execução.
- O resumo e as execuções usarão códigos e unidades nutricionais do catálogo; a importação deverá manter unidade consistente por código de nutriente.
- Autorização permanecerá centrada no paciente dono do dado; leituras de nutricionista seguirão o padrão de vínculo ativo já existente, quando aplicável.

## Testes

- Busca por termo com paginação, alimento inativo, acentos e ausência de resultados.
- Calendário com planos, prescrições e datas livres de execução.
- Metas por versão publicada, resumo com e sem plano, distinção entre meta/planejado/realizado e nutrientes ausentes como zero.
- CRUD de execução em cada modo, incluindo validação de alimento, porção, substituição e propriedade.
- Execução livre em qualquer data apenas sob prescrição publicada `FREE`.
- Realocação válida, destinos inválidos, conflito concorrente e repetição idempotente.
- Soft delete, restauração dentro de cinco segundos, expiração e repetição de restauração.
- Regressão de OpenAPI, migrations, persistência e arquitetura.

## Premissas confirmadas

- Não há produção nem necessidade de compatibilidade com `completion-events`.
- A entrega será única.
- A prescrição publicada atual define a permissão para execuções livres fora de plano.
- Remoções são persistidas temporariamente e só podem ser desfeitas por cinco segundos.
