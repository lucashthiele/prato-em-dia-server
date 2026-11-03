# Infraestrutura e deploy

Decisões de infraestrutura do piloto, tomadas em 2026-11-02. A meta é usar a VPS da Hostinger (já contratada, com o domínio) e free tier para o resto. A planilha de projeção financeira descreve a visão de escala (DigitalOcean 4 vCPU/8 GB, PostgreSQL gerenciado, hospedagem separada do portal); este documento descreve o ponto de partida.

## Topologia

Docker Compose na VPS, com três serviços:

- **`caddy`**: único serviço com portas publicadas (80 e 443). Termina HTTPS (Let's Encrypt), serve os arquivos estáticos do portal em `/` e encaminha `/api/*` para a cor ativa do `app`. Portal e API ficam no mesmo origin (ver ADR 0001 e o ADR 0008 do portal).
- **`app-blue` / `app-green`**: a mesma imagem Spring Boot com tags diferentes (ADR 0003). O Caddy lê o upstream ativo de um arquivo incluído (`upstream.caddy`) e a virada é `caddy reload`.
- **`postgres`**: na rede interna do Compose, sem porta publicada.

Serviços externos gratuitos: GitHub Actions (CI), GHCR (imagens), Resend free (e-mails), Cloudflare R2 ou Backblaze B2 (backup), Grafana Cloud free (monitoramento) e um monitor externo de uptime com heartbeat.

## Perfis e configuração

- `local`: o atual, com `compose.yaml` de desenvolvimento e `dev-seed`.
- `prod`: `ddl-auto=validate` e `spring.flyway.validate-on-migrate=true`.
- Banco por `DATABASE_URL`, `DATABASE_USERNAME` e `DATABASE_PASSWORD`. Migrar para um banco gerenciado no futuro é só trocar essas variáveis. O perfil `supabase` do LUC-47 foi descartado.
- Segredos da aplicação (`JWT_SECRET`, `RESEND_API_KEY`, senha do banco) ficam num `.env` só na VPS, fora do GitHub e da imagem.
- O Actuator usa uma porta de gerenciamento própria (por exemplo `8081`), nunca publicada. O `readiness` e as métricas Prometheus são consultados de dentro da rede do Compose.

## Pipeline e deploy

- **Dono do deploy:** o repositório do servidor. Um manifesto de release (`deploy/release.yaml`) fixa a versão da API (SHA da imagem) e a versão do portal (SHA do build). O portal só constrói e publica o artefato.
- **Gatilho:** merge em `main` depois do CI (`clean verify`, formatação, migrations).
- **Build fora da VPS:** a GHA constrói a imagem e publica no GHCR com tag do SHA.
- **Deploy por SSH** com usuário dedicado e chave restrita (secret do ambiente `production`):
  1. sobe a cor ociosa com a imagem nova (o Flyway roda na subida);
  2. espera o `readiness`, com timeout generoso (a subida é mais lenta com mais classes);
  3. envia a release do portal para `/srv/portal/releases/<sha>`;
  4. vira o Caddy e o symlink `/srv/portal/current` juntos;
  5. mantém a cor antiga ligada.
- **Rollback automático** se o `readiness` não ficar verde: nada muda para o usuário. Rollback manual é reverter o manifesto, ou voltar o upstream e o symlink para as versões anteriores.
- Sem aprovação manual por enquanto. Ela pode entrar depois pelo ambiente `production` do GitHub.

## Compatibilidade entre API e clientes

- Mudança de contrato é aditiva por padrão. Remover ou renomear campo ou rota acontece em duas releases (depreciar, depois remover).
- O deploy é em bloco (API mais portal), mas abas abertas e versões antigas do app continuam existindo. Por isso web e mobile enviam `X-Client-Version`, o servidor tem uma versão mínima por plataforma em configuração, e abaixo dela responde `426` com o código `CLIENT_OUTDATED`.
- O build do portal gera `version.json` (sem cache) e o portal avisa "Nova versão disponível" ao voltar o foco.
- Antes do go-live do piloto as quebras únicas (como a dos cookies) estão liberadas.

## Backup

- `pg_dump` compactado e **criptografado com `age`** na VPS antes do envio. A chave privada fica só com o responsável, fora da VPS e do provedor.
- Diário às 03:00 BRT. Retenção: 7 diários, 4 semanais e 3 mensais, por regra de ciclo de vida do bucket.
- Uma vez por mês (e antes do go-live) restaurar o último dump num Postgres descartável e verificar contagem de linhas e o schema no Flyway. O resultado vai para o Discord.
- Falha do backup alerta no Discord, e o heartbeat do monitor externo alerta se o backup **não** rodar.
- O runbook de restauração também serve de "rollback documentado" do Marco 5.

## Observabilidade e alertas

- Actuator e Micrometer expõem métricas Prometheus na porta de gerenciamento. Um container Grafana Alloy envia métricas e logs ao Grafana Cloud.
- Eventos críticos viram métricas e regras de alerta, entregues por **webhook do Discord**: e-mail em `DEAD` no outbox, falha das três tentativas de geração, falha de backup, rollback de deploy.
- Um monitor externo gratuito (UptimeRobot ou Better Stack) verifica a API a cada poucos minutos. Confirmar os limites e a licença de uso comercial na página oficial de cada um antes de contratar, porque variam entre as fontes.
- Medir o consumo de memória do Alloy na VPS durante a implementação.

## Segurança da VPS (Marco 5)

Firewall com apenas 22, 80 e 443, SSH somente por chave e sem login de root, banco sem exposição pública, usuário de deploy com permissões mínimas. Limitação de tentativas de login ainda não está definida (ver o roadmap).
