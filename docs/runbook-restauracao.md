# Runbook de restauração do banco

Serve para o **teste mensal de restauração** e para a recuperação de desastre. É também o "rollback documentado" do Marco 5.
Pré-requisitos: a chave privada `age` (nunca fica na VPS), Docker na máquina que restaura e acesso de leitura ao bucket.

## Teste mensal (e antes do go-live)

O script `deploy/backup/restore-test.sh` baixa o dump mais recente, decifra, restaura num Postgres **descartável**, confere o Flyway (nenhuma migration com falha, versão mais recente) e a contagem de linhas por tabela, e envia o resultado ao Discord.

```shell
docker build -t prato-backup deploy/backup
docker network create restore-net
docker run -d --name restore-pg --network restore-net -e POSTGRES_PASSWORD=pw postgres:17-alpine

docker run --rm --network restore-net \
  -v /caminho/prato-backup.agekey:/identity.txt:ro \
  -e BACKUP_SOURCE=r2:prato-backups/daily \
  -e AGE_IDENTITY_FILE=/identity.txt \
  -e RCLONE_CONFIG_R2_TYPE=s3 -e RCLONE_CONFIG_R2_PROVIDER=Cloudflare \
  -e RCLONE_CONFIG_R2_ACCESS_KEY_ID=... -e RCLONE_CONFIG_R2_SECRET_ACCESS_KEY=... \
  -e RCLONE_CONFIG_R2_ENDPOINT=https://<conta>.r2.cloudflarestorage.com \
  -e PGHOST=restore-pg -e PGUSER=postgres -e PGPASSWORD=pw \
  -e DISCORD_WEBHOOK_URL=https://discord.com/api/webhooks/... \
  prato-backup /usr/local/bin/restore-test.sh

docker rm -f restore-pg && docker network rm restore-net
```

A saída traz `arquivo=`, `flyway_latest_version=` e `tabela=contagem`. Compare com a produção
(`select max(version::int) from flyway_schema_history where success`). Se o teste falhar, trate como incidente: o backup não é confiável.

## Recuperação de desastre

1. Subir uma VPS nova e aplicar o endurecimento ([trabalho manual](trabalho-manual/w4-infra.md), LUC-72).
2. Copiar `deploy/` e o `.env` (do cofre de senhas) para `/srv/prato`.
3. Subir só o banco: `docker compose up -d postgres`.
4. Baixar e decifrar o dump (na máquina com a chave privada) e copiar para a VPS:
   `rclone copyto r2:prato-backups/daily/<arquivo> dump.age && age -d -i prato-backup.agekey -o dump dump.age`.
5. Restaurar: `docker compose exec -T postgres pg_restore --clean --if-exists --no-owner -U pratoemdia -d pratoemdia < dump`.
6. Subir a API (`deploy.sh`) e conferir o readiness. O Flyway aplica as migrations que faltarem.
7. Apontar o DNS para a VPS nova e conferir o fluxo de login.

A perda máxima de dados é a de até 24 horas (intervalo entre backups).
