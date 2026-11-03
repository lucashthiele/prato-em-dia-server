# Trabalho manual: infraestrutura e documentação (W4)

Tudo aqui exige conta, segredo, painel, acesso à VPS ou conferência humana, então o código do repositório não consegue fazer.
Marque cada item ao concluir. As decisões de fundo estão em [infraestrutura.md](../infraestrutura.md) e no [ADR 0003](../adr/0003-blue-green-e-migrations-em-duas-etapas.md).

Ordem sugerida: LUC-72 (VPS) → LUC-47/46 (`.env`) → LUC-48 (pipeline) → LUC-68 (backup) → LUC-69 (observabilidade) → LUC-50/27 e LUC-49.

> Os comandos `docker compose` abaixo rodam em `/srv/prato`. Para não esquecer as variáveis, use
> `alias dc='docker compose --env-file .env --env-file images.env'` (o `images.env` guarda a imagem de cada cor).

## Pré-requisitos (uma vez)

- [ ] VPS da Hostinger com Ubuntu LTS, acesso root inicial por SSH e IP público anotado.
- [ ] Domínio com registro `A` (e `AAAA`, se houver IPv6) apontando para a VPS. Confira: `dig +short <dominio>` devolve o IP da VPS.
- [ ] Docker Engine e o plugin Compose instalados na VPS: `docker compose version` funciona.
- [ ] Um usuário administrador (não root) com chave SSH, no grupo `sudo`: `ssh <admin>@<ip>` entra sem senha e `sudo -v` funciona.

## LUC-72: endurecimento da VPS

Arquivos: `deploy/vps/harden.sh`, `deploy/vps/ssh-gate.sh`, `deploy/vps/verify-hardening.sh`.

- [ ] Gerar o par de chaves de deploy **na sua máquina**: `ssh-keygen -t ed25519 -f ~/.ssh/prato_deploy -C deploy@gha -N ''`. A privada vira o secret `DEPLOY_SSH_KEY` (abaixo); nunca vai para a VPS.
- [ ] Copiar o repositório (ou só `deploy/vps/`) para a VPS e rodar como root, com a chave **pública** do deploy:
  `DEPLOY_PUBKEY="$(cat ~/.ssh/prato_deploy.pub)" ADMIN_USER=<admin> sudo -E deploy/vps/harden.sh`
- [ ] **Antes de fechar a sessão atual**, abrir um segundo terminal e confirmar `ssh <admin>@<ip>`. Só então encerrar a sessão antiga.
- [ ] Rodar `sudo deploy/vps/verify-hardening.sh` na VPS: todas as linhas `ok`.
- [ ] De fora da VPS, rodar `deploy/vps/verify-hardening.sh --remote <ip>`: 22, 80 e 443 abertas; 5432, 8080, 8081 e 12345 fechadas. (O Docker ignora o `ufw` ao publicar portas, por isso o verify também confere `docker ps`.)
- [ ] Confirmar que o login por senha e o de root falham: `ssh -o PubkeyAuthentication=no <admin>@<ip>` e `ssh root@<ip>` recusam.
- [ ] Confirmar que o usuário `deploy` só roda os comandos permitidos: `ssh -i ~/.ssh/prato_deploy deploy@<ip> bash` deve responder "comando não permitido".
- [ ] Anotar a impressão digital do host para o secret `DEPLOY_KNOWN_HOSTS`: `ssh-keyscan -t ed25519 <ip>`.
- [ ] Decidir sobre a limitação de tentativas de login da API (LUC-73, sem desenho ainda): o `fail2ban` cobre só o SSH.

## LUC-47: perfil `prod` e `.env`

- [ ] Criar `/srv/prato/.env` (modo `600`, dono `deploy`) a partir de `deploy/env.example`, com segredos reais: `POSTGRES_PASSWORD`, `JWT_SECRET` (32 bytes ou mais, por exemplo `openssl rand -hex 32`), `RESEND_API_KEY`, `MAIL_FROM`, `APP_URL`, `DOMAIN`. `chmod 600 /srv/prato/.env`.
- [ ] No Resend: verificar o domínio do `MAIL_FROM` (SPF e DKIM no DNS) e criar a chave de API de produção.
- [ ] Nunca versionar o `.env`: `git check-ignore .env` deve confirmar (o `.gitignore` ignora `*.env`).

## LUC-46: Actuator

- [ ] Na VPS, depois do primeiro deploy: `docker compose -f /srv/prato/compose.yaml exec app-blue wget -qO- http://localhost:8081/actuator/health/readiness` devolve `{"status":"UP"}`.
- [ ] Conferir que a porta 8081 não está publicada: `ss -ltnp | grep 8081` não mostra nada, e `verify-hardening.sh --remote` confirma de fora.

## LUC-67: ShedLock

- [ ] Depois do primeiro deploy, conferir a tabela: `docker compose exec postgres psql -U pratoemdia -d pratoemdia -c 'select * from shedlock'`.
- [ ] Com as duas cores ligadas, às 08:00, 08:10 e 08:20 BRT (e a cada 5 minutos na confirmação) conferir no log que só **uma** cor executa o job: procure por `weeklyPlan` nos logs de `app-blue` e `app-green`.

## LUC-48: pipeline, imagem e deploy

Arquivos: `.github/workflows/maven.yml` (CI), `.github/workflows/deploy.yml`, `Dockerfile`, `deploy/`.

- [ ] GitHub, **Settings > Environments**: criar o ambiente `production` (sem aprovação manual por enquanto).
- [ ] Secrets do ambiente `production`: `DEPLOY_SSH_KEY` (conteúdo de `~/.ssh/prato_deploy`), `DEPLOY_KNOWN_HOSTS` (saída do `ssh-keyscan`), `DEPLOY_HOST` (IP ou domínio), `DEPLOY_USER` (`deploy`), `PORTAL_REPO_TOKEN` (token com leitura de releases do repositório do portal).
- [ ] Variável do ambiente `production`: `PORTAL_REPO` (por exemplo `lucashthiele/prato-em-dia-portal`).
- [ ] **Settings > Actions > General > Workflow permissions**: permitir leitura e escrita (o workflow publica no GHCR com o `GITHUB_TOKEN`).
- [ ] Combinar com o repositório do portal: o build precisa publicar uma release com a tag igual ao SHA do portal e o arquivo `portal-<sha>.tar.gz` (conteúdo da pasta `dist`, com `index.html` e `version.json`). Sem isso o passo "Download portal build" falha.
- [ ] Editar `deploy/release.yaml`: trocar `REPLACE_WITH_PORTAL_SHA` pelo SHA real do portal.
- [ ] Na VPS, preparar o estado inicial (como `deploy`): `mkdir -p /srv/prato /srv/portal/releases`; criar `/srv/prato/upstream.caddy` com `reverse_proxy app-blue:8080` e `echo blue > /srv/prato/active_color`; copiar `deploy/compose.yaml`, `deploy/Caddyfile` e `deploy/scripts/*` para `/srv/prato/` (o primeiro `deploy.yml` também faz isso por rsync).
- [ ] Primeiro deploy: o `docker login ghcr.io` precisa estar feito na VPS, usando um token `read:packages`, como `deploy` ou o usuário que roda o Compose (ou tornar a imagem pública). Confirmar `docker pull ghcr.io/<dono>/prato-em-dia-server:<sha>` na VPS.
- [ ] Subir os serviços fixos: `cd /srv/prato && docker compose --env-file .env --env-file images.env up -d caddy postgres` (o `images.env` é criado pelo primeiro `deploy.sh`; crie um vazio antes com `touch images.env`).
- [ ] Rodar o workflow `Deploy` manualmente (**Actions > Deploy > Run workflow**) e conferir: `curl -fsS https://<dominio>/api/v1/auth/login` responde 4xx do envelope (não 502), o portal abre em `https://<dominio>/` e a cor ativa mudou (`cat /srv/prato/active_color`).
- [ ] Testar o rollback automático: publicar uma imagem que não sobe (por exemplo, `JWT_SECRET` ausente numa cópia) e confirmar que o deploy falha, a cor ativa continua a mesma e o Discord recebe o aviso.
- [ ] Proteger `main` (**Settings > Branches**): exigir o CI `Build and Test` verde antes do merge.
- [ ] Revisar `docs/infraestrutura.md` depois do primeiro deploy e ajustar o que divergiu.

## LUC-68: backup diário criptografado

Arquivos: `deploy/backup/` (imagem, `backup.sh`, `restore-test.sh`), `deploy/tests/backup_test.sh`, [runbook de restauração](../runbook-restauracao.md).

- [ ] Criar o par de chaves `age` **na sua máquina** (não na VPS): `age-keygen -o prato-backup.agekey`. Guardar o arquivo em dois lugares seguros fora da VPS e do provedor (gerenciador de senhas e um backup offline). **Sem esta chave os dumps são irrecuperáveis.**
- [ ] Colocar só a chave **pública** (`age1...`) em `AGE_RECIPIENT` no `.env` da VPS.
- [ ] Criar o bucket (Cloudflare R2 ou Backblaze B2), **privado**, e uma chave de API restrita a esse bucket. Preencher no `.env` as variáveis `RCLONE_CONFIG_R2_*` e `BACKUP_DEST` (veja `deploy/env.example`; para B2 troque o tipo e o provedor).
- [ ] Regras de ciclo de vida do bucket por prefixo: `daily/` expira em 7 dias, `weekly/` em 28 dias, `monthly/` em 90 dias.
- [ ] Criar o webhook do Discord (canal de alertas: **Editar canal > Integrações > Webhooks**) e colocar em `DISCORD_WEBHOOK_URL`.
- [ ] No monitor de heartbeat (Better Stack ou UptimeRobot): criar um heartbeat com período de 24 h e tolerância curta, copiar a URL para `HEARTBEAT_URL` e configurar o alerta no Discord. Confirmar os limites e a licença de uso comercial na página oficial antes de contratar.
- [ ] Subir o serviço: `docker compose up -d --build backup`. O log mostra "backup agendado para 03:00".
- [ ] Rodar um backup agora: `docker compose exec backup /usr/local/bin/backup.sh`. Conferir: arquivo em `daily/` no bucket, ping no monitor, nenhuma mensagem de falha.
- [ ] Provar o alarme de falha: rodar o backup com `PGPASSWORD` errado (`docker compose exec -e PGPASSWORD=x backup /usr/local/bin/backup.sh`) e conferir a mensagem no Discord.
- [ ] Provar o alarme de ausência: pausar o heartbeat e conferir que o monitor avisa no Discord.
- [ ] **Teste de restauração** (antes do go-live e uma vez por mês, na máquina que tem a chave privada): siga o [runbook](../runbook-restauracao.md), confira o resultado no Discord e registre a data aqui: ____ / ____ / ____.
- [ ] Anotar o responsável pela chave `age` e um plano de sucessão (quem recebe a chave se o responsável ficar indisponível).

## LUC-69: observabilidade (Grafana Cloud e Discord)

Arquivos: `deploy/observability/` (`config.alloy`, `alert-rules.yaml`, `contact-point-discord.yaml`), serviço `alloy` no `deploy/compose.yaml`, `CriticalEventMetrics` no código.

- [ ] Criar a conta Grafana Cloud (free) e uma stack. Em **Connections**, anotar: URL e usuário do Prometheus (`remote_write`) e URL e usuário do Loki. Criar um **Access Policy token** com escopos `metrics:write`, `logs:write` e `rules:write` (para o `mimirtool`).
- [ ] Preencher no `.env` da VPS as variáveis `GRAFANA_CLOUD_*` (veja `deploy/env.example`).
- [ ] Subir o Alloy: `docker compose up -d alloy`. Conferir no Grafana (**Explore**): `up{job="prato-api"}` devolve 1 para a cor ativa e 0 para a ociosa, e o Loki mostra logs com o rótulo `container`.
- [ ] **Medir a memória do Alloy** durante alguns dias (`docker stats alloy`) e ajustar `mem_limit` e `GOMEMLIMIT` no compose; registrar o valor observado: ______ MiB.
- [ ] Importar o contact point do Discord: o `contact-point-discord.yaml` usa `${DISCORD_WEBHOOK_URL}`; no Grafana, **Alerting > Contact points > Add** (tipo Discord) com a URL real, e defina `discord-prato` como contact point padrão da política de notificação. Use o **Test** e confira a mensagem no canal.
- [ ] Carregar as regras: `mimirtool rules load --address=<URL do ruler> --id=<usuário Prometheus> --key=<token> deploy/observability/alert-rules.yaml`. Conferir em **Alerting > Alert rules**.
- [ ] Provar cada alerta crítico em ambiente controlado: derrubar as duas cores por 2 minutos (`ApiDown`) e conferir o Discord.
- [ ] **Ligar os contadores no código** (feito na integração, fora do W4): chamar `CriticalEventMetrics.emailOutboxDead()` quando um e-mail virar `DEAD` (LUC-66) e `CriticalEventMetrics.planGenerationFailed()` quando a terceira tentativa de geração falhar (LUC-39). Sem essas chamadas as regras `EmailOutboxDead` e `PlanGenerationFailed` nunca disparam.
- [ ] Monitor externo de uptime (UptimeRobot ou Better Stack) apontando para `https://<dominio>/` e `https://<dominio>/api/v1/...` (uma rota pública), alerta no Discord. Confirmar os limites do plano gratuito na página oficial.
- [ ] Alerta de rollback de deploy: sai direto do `deploy.sh` para o Discord, que lê `DISCORD_WEBHOOK_URL` do `/srv/prato/.env`. Provar com um deploy que falha (ver LUC-48).

## LUC-50 e LUC-27: catálogo de erros e contrato OpenAPI

- [ ] **Decisões de produto/clientes** (conferir antes de integrar):
  - `CONSENT_REQUIRED` está catalogado como **403** (mesmo padrão do `PASSWORD_CHANGE_REQUIRED`); confirmar com quem implementa o LUC-65.
  - O `409` de geração concorrente usa o código `PLAN_GENERATION_IN_PROGRESS`; quem implementa o LUC-39 deve usar exatamente esse código.
  - `NO_ALLOWED_FOODS` é um **motivo** (`blockedReason`), não um erro HTTP.
- [ ] Na integração, cada worker que criar `CodedApplicationException` adiciona seu código na `*ErrorCatalogSection` da feature (o `ErrorCatalogTest` falha se faltar).
- [ ] Depois do LUC-54 (cookies), conferir no Swagger UI de um ambiente real que o esquema `bearerAuth` virou esquema de cookie e que o login pelo "Try it out" funciona; atualizar `OpenApiIntegrationTest` se o nome do esquema mudar.
- [ ] Revisar as descrições de operações de planejamento (por exemplo "para os sete dias seguintes") depois que o LUC-39 mudar o período.

## LUC-49: documentação

Corrigido neste repositório: [openapi.md](../openapi.md) (autenticação por cookies, sem Authorize por token), [erros.md](../erros.md) (envelope não é RFC 9457, parâmetro `q`), [migrations.md](../migrations.md) (V001 a V025 com `meal_execution`, sem append-only).

Os documentos abaixo **não estão neste repositório** (documento de arquitetura, plano consolidado e documento dos marcos). Corrigir à mão onde eles vivem:

- [ ] Documento de arquitetura: trocar "erros em `application/problem+json`" por "envelope `{status, code, message, errors[]}` em `application/json`" e apontar para `docs/erros.md`.
- [ ] Plano consolidado: trocar o parâmetro `query` por `q` na busca de alimentos (`GET /api/v1/foods?q=`).
- [ ] Documentos de banco/modelo: trocar "V001 a V013" e "eventos append-only" por "V001 a V025+" com `meal_execution` (V016), remoção dos eventos (V018) e as migrations de infraestrutura V060+.
- [ ] Marco 5: remover a citação de "assinatura simulada" (fora do escopo, LUC-31) e incluir o rollback documentado ([runbook de restauração](../runbook-restauracao.md)) e o endurecimento da VPS (LUC-72) nos critérios.
- [ ] Depois do go-live, revisar [infraestrutura.md](../infraestrutura.md) com o que mudou na prática.
